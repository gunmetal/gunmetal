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
    private final DependencyServiceFactory dependencyServiceFactory;
    private final ConverterSupplier converterSupplier;
    private final ComponentRepository componentRepository;
    private final ComponentContext context;
    private final boolean requireInterfaces;

    ComponentDependencySupplier(SupplierAdapter supplierAdapter,
                                DependencyServiceFactory dependencyServiceFactory,
                                ConverterSupplier converterSupplier,
                                ComponentRepository componentRepository,
                                ComponentContext context,
                                boolean requireInterfaces) {
        this.supplierAdapter = supplierAdapter;
        this.dependencyServiceFactory = dependencyServiceFactory;
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
        DependencyService dependencyService = dependencyServiceFactory.createJit(dependencyRequest, context);
        if (dependencyService != null) {
            componentRepository.put(dependencyRequest.dependency(), dependencyService, context.errors());
            return dependencyService
                    .service(dependencyRequest, context.errors())
                    .provisionStrategy();
        }

        // try conversion strategy
        TypeKey typeKey = dependency.typeKey();
        for (Converter converter : converterSupplier.convertersForType(typeKey)) {
            for (Class<?> fromType : converter.supportedFromTypes()) {
                dependencyService = createConversionDependencyService(converter, fromType, dependency);
                if (dependencyService != null) {
                    componentRepository.put(dependency, dependencyService, context.errors());
                    return dependencyService
                            .service(dependencyRequest, context.errors())
                            .provisionStrategy();
                }
            }
        }

        // try jit local factory method dependencyService
        List<DependencyService> factoryDependencyServicesForRequest =
                dependencyServiceFactory.createJitFactoryRequest(dependencyRequest, context);
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
        DependencyService dependencyService = componentRepository.get(dependency);
        if (dependencyService != null) {
            return dependencyService
                    .service(dependencyRequest, context.errors())
                    .provisionStrategy();
        }
        if (supplierAdapter.isSupplier(dependency)) {
            dependencyService = createReferenceDependencyService(
                    dependencyRequest, () -> new SupplierStrategyFactory(supplierAdapter));
            if (dependencyService != null) {
                componentRepository.put(dependency, dependencyService, context.errors());
                return dependencyService
                        .service(dependencyRequest, context.errors())
                        .provisionStrategy();
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
            dependencyService = createReferenceDependencyService(dependencyRequest, RefStrategyFactory::new);
            if (dependencyService != null) {
                componentRepository.put(dependency, dependencyService, context.errors());
                return dependencyService
                        .service(dependencyRequest, context.errors())
                        .provisionStrategy();
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

    private DependencyService createReferenceDependencyService(
            final DependencyRequest refRequest, Supplier<ReferenceStrategyFactory> factorySupplier) {
        Dependency referenceDependency = refRequest.dependency();
        Type providedType = ((ParameterizedType) referenceDependency.typeKey().type()).getActualTypeArguments()[0];
        final Dependency provisionDependency = Dependency.from(referenceDependency.qualifier(), providedType);
        DependencyService provisionDependencyService = componentRepository.get(provisionDependency);
        if (provisionDependencyService == null) {
             return null;
        }
        ProvisionStrategy provisionStrategy = provisionDependencyService.force();
        ReferenceStrategyFactory strategyFactory = factorySupplier.get();
        final ProvisionStrategy referenceStrategy = strategyFactory.create(provisionStrategy, this);
        return dependencyServiceFactory.createForReference(
                refRequest, provisionDependencyService, provisionDependency, referenceStrategy, strategyFactory);
    }

    private DependencyService createConversionDependencyService(
            Converter converter, Class<?> fromType, Dependency to) {
        Dependency from = Dependency.from(to.qualifier(), fromType);
        DependencyService fromDependencyService = componentRepository.get(from);
        if (fromDependencyService != null) {
            return dependencyServiceFactory.createForConversion(fromDependencyService, converter, from, to);
        }
        return null;
    }

}
