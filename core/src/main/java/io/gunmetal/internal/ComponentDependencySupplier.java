package io.gunmetal.internal;

import io.gunmetal.MultiBind;
import io.gunmetal.Param;
import io.gunmetal.Ref;
import io.gunmetal.spi.Converter;
import io.gunmetal.spi.ConverterSupplier;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.SupplierAdapter;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.TypeKey;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Instances of the supplier should not be accessed concurrently.
 *
 * @author rees.byars
 */
class ComponentDependencySupplier implements DependencySupplier {

    private final SupplierAdapter supplierAdapter;
    private final ResourceAccessorFactory resourceAccessorFactory;
    private final ConverterSupplier converterSupplier;
    private final ComponentRepository componentRepository;
    private final ComponentContext context;
    private final boolean requireInterfaces;

    ComponentDependencySupplier(SupplierAdapter supplierAdapter,
                                ResourceAccessorFactory resourceAccessorFactory,
                                ConverterSupplier converterSupplier,
                                ComponentRepository componentRepository,
                                ComponentContext context,
                                boolean requireInterfaces) {
        this.supplierAdapter = supplierAdapter;
        this.resourceAccessorFactory = resourceAccessorFactory;
        this.converterSupplier = converterSupplier;
        this.componentRepository = componentRepository;
        this.context = context;
        this.requireInterfaces = requireInterfaces;
    }

    @Override public synchronized ProvisionStrategy getProvisionStrategy(
            final DependencyRequest dependencyRequest) {

        Dependency dependency = dependencyRequest.dependency();

        // TODO param check is nasty.  wrap qualifier in DependencyMetadata class and resolve this in resolver?
        // TODO add visitor for param request and decorator for param provision
        if (Arrays.stream(dependency.qualifier().qualifiers()).anyMatch(q -> q instanceof Param)) {
            return (supplier, resolutionContext) -> resolutionContext.getParam(dependencyRequest.dependency());
        }

        // try cached strategy
        ProvisionStrategy strategy = getCachedProvisionStrategy(dependencyRequest);
        if (strategy != null) {
            return strategy;
        }

        // try jit constructor dependencyService strategy
        ResourceAccessor resourceAccessor = resourceAccessorFactory.createJit(dependencyRequest, context);
        if (resourceAccessor != null) {
            componentRepository.put(dependencyRequest.dependency(), resourceAccessor, context.errors());
            return resourceAccessor
                    .process(dependencyRequest, context.errors());
        }

        // try conversion strategy
        TypeKey typeKey = dependency.typeKey();
        for (Converter converter : converterSupplier.convertersForType(typeKey)) {
            for (Class<?> fromType : converter.supportedFromTypes()) {
                resourceAccessor = createConversionDependencyService(converter, fromType, dependency);
                if (resourceAccessor != null) {
                    componentRepository.put(dependency, resourceAccessor, context.errors());
                    return resourceAccessor
                            .process(dependencyRequest, context.errors());
                }
            }
        }

        // try jit local factory method dependencyService
        List<ResourceAccessor> factoryDependencyServicesForRequest =
                resourceAccessorFactory.createJitFactoryRequest(dependencyRequest, context);
        componentRepository.putAll(factoryDependencyServicesForRequest, context.errors());
        strategy = getCachedProvisionStrategy(dependencyRequest);
        if (strategy != null) {
            return strategy;
        }

        // all attempts to serve request have failed
        context.errors().add(
                dependencyRequest.sourceProvision(),
                "There is no provider defined for a dependency -> " + dependencyRequest.dependency());

        // TODO shouldn't need to cast
        return (p, c) -> {
            ((ComponentErrors) context.errors()).throwIfNotEmpty();
            return null;
        };

    }

    public synchronized ProvisionStrategy getCachedProvisionStrategy(final DependencyRequest dependencyRequest) {

        final Dependency dependency = dependencyRequest.dependency();

        if (requireInterfaces &&
                !(dependency.typeKey().raw().isInterface()
                        || dependencyRequest.sourceProvision().overrides().allowNonInterface())) {
            context.errors().add(
                    dependencyRequest.sourceProvision(),
                    "Dependency is not an interface -> " + dependency);
        }
        ResourceAccessor resourceAccessor = componentRepository.get(dependency);
        if (resourceAccessor != null) {
            return resourceAccessor
                    .process(dependencyRequest, context.errors());
        }
        if (supplierAdapter.isSupplier(dependency)) {
            resourceAccessor = createReferenceDependencyService(
                    dependencyRequest, () -> new SupplierStrategyFactory(supplierAdapter));
            if (resourceAccessor != null) {
                componentRepository.put(dependency, resourceAccessor, context.errors());
                return resourceAccessor
                        .process(dependencyRequest, context.errors());
            } else {
                // support empty multi-bind request
                // TODO should not know about MultiBind here -> should be included in above mentioned DependencyMetadata
                if (Arrays.stream(dependency.qualifier().qualifiers()).anyMatch(q -> q instanceof MultiBind)) {
                    return (supplier, resolutionContext) ->
                            supplierAdapter.supplier(ArrayList::new);
                }
            }
        }
        if (dependency.typeKey().raw() == Ref.class) {
            resourceAccessor = createReferenceDependencyService(dependencyRequest, RefStrategyFactory::new);
            if (resourceAccessor != null) {
                componentRepository.put(dependency, resourceAccessor, context.errors());
                return resourceAccessor
                        .process(dependencyRequest, context.errors());
            }  else {
                // support empty multi-bind request
                // TODO should not know about MultiBind here -> should be included in above mentioned DependencyMetadata
                if (Arrays.stream(dependency.qualifier().qualifiers()).anyMatch(q -> q instanceof MultiBind)) {
                    return (supplier, resolutionContext) -> (Ref<Object>) ArrayList::new;
                }
            }
        }

        // support empty multi-bind request
        // TODO should not know about MultiBind here -> should be included in above mentioned DependencyMetadata
        if (Arrays.stream(dependency.qualifier().qualifiers()).anyMatch(q -> q instanceof MultiBind)) {
            return (supplier, resolutionContext) -> new ArrayList<>();
        }

        return null;
    }

    private ResourceAccessor createReferenceDependencyService(
            final DependencyRequest refRequest, Supplier<ReferenceStrategyFactory> factorySupplier) {
        Dependency referenceDependency = refRequest.dependency();
        Type providedType = ((ParameterizedType) referenceDependency.typeKey().type()).getActualTypeArguments()[0];
        final Dependency provisionDependency = Dependency.from(referenceDependency.qualifier(), providedType);
        ResourceAccessor provisionResourceAccessor = componentRepository.get(provisionDependency);
        if (provisionResourceAccessor == null) {
             return null;
        }
        ProvisionStrategy provisionStrategy = provisionResourceAccessor.force();
        ReferenceStrategyFactory strategyFactory = factorySupplier.get();
        final ProvisionStrategy referenceStrategy = strategyFactory.create(provisionStrategy, this);
        return resourceAccessorFactory.createForReference(
                refRequest, provisionResourceAccessor, provisionDependency, referenceStrategy, strategyFactory);
    }

    private ResourceAccessor createConversionDependencyService(
            Converter converter, Class<?> fromType, Dependency to) {
        Dependency from = Dependency.from(to.qualifier(), fromType);
        ResourceAccessor fromResourceAccessor = componentRepository.get(from);
        if (fromResourceAccessor != null) {
            return resourceAccessorFactory.createForConversion(fromResourceAccessor, converter, from, to);
        }
        return null;
    }

}
