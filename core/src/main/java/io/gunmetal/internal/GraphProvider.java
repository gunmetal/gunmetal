package io.gunmetal.internal;

import io.gunmetal.MultiBind;
import io.gunmetal.Param;
import io.gunmetal.Provider;
import io.gunmetal.Ref;
import io.gunmetal.spi.Converter;
import io.gunmetal.spi.ConverterProvider;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProviderAdapter;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.TypeKey;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Instances of the provider should not be accessed concurrently.
 *
 * @author rees.byars
 */
class GraphProvider implements InternalProvider {

    private final ProviderAdapter providerAdapter;
    private final DependencyServiceFactory dependencyServiceFactory;
    private final ConverterProvider converterProvider;
    private final GraphCache graphCache;
    private final GraphContext context;
    private final boolean requireInterfaces;

    GraphProvider(ProviderAdapter providerAdapter,
                  DependencyServiceFactory dependencyServiceFactory,
                  ConverterProvider converterProvider,
                  GraphCache graphCache,
                  GraphContext context,
                  boolean requireInterfaces) {
        this.providerAdapter = providerAdapter;
        this.dependencyServiceFactory = dependencyServiceFactory;
        this.converterProvider = converterProvider;
        this.graphCache = graphCache;
        this.context = context;
        this.requireInterfaces = requireInterfaces;
    }

    @Override public synchronized ProvisionStrategy getProvisionStrategy(
            final DependencyRequest dependencyRequest) {

        Dependency dependency = dependencyRequest.dependency();

        // TODO param check is nasty.  wrap qualifier in DependencyMetadata class and resolve this in resolver?
        // TODO add visitor for param request and decorator for param provision
        if (Arrays.stream(dependency.qualifier().qualifiers()).anyMatch(q -> q instanceof Param)) {
            return (internalProvider, resolutionContext) -> resolutionContext.getParam(dependencyRequest.dependency());
        }

        // try cached strategy
        ProvisionStrategy strategy = getCachedProvisionStrategy(dependencyRequest);
        if (strategy != null) {
            return strategy;
        }

        // try jit constructor dependencyService strategy
        DependencyService dependencyService = dependencyServiceFactory.createJit(dependencyRequest, context);
        if (dependencyService != null) {
            graphCache.put(dependencyRequest.dependency(), dependencyService, context.errors());
            return dependencyService
                    .service(dependencyRequest, context.errors())
                    .provisionStrategy();
        }

        // try conversion strategy
        TypeKey typeKey = dependency.typeKey();
        for (Converter converter : converterProvider.convertersForType(typeKey)) {
            for (Class<?> fromType : converter.supportedFromTypes()) {
                dependencyService = createConversionDependencyService(converter, fromType, dependency);
                if (dependencyService != null) {
                    graphCache.put(dependency, dependencyService, context.errors());
                    return dependencyService
                            .service(dependencyRequest, context.errors())
                            .provisionStrategy();
                }
            }
        }

        // try jit local factory method dependencyService
        List<DependencyService> factoryDependencyServicesForRequest =
                dependencyServiceFactory.createJitFactoryRequest(dependencyRequest, context);
        graphCache.putAll(factoryDependencyServicesForRequest, context.errors());
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
            ((GraphErrors) context.errors()).throwIfNotEmpty();
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
        DependencyService dependencyService = graphCache.get(dependency);
        if (dependencyService != null) {
            return dependencyService
                    .service(dependencyRequest, context.errors())
                    .provisionStrategy();
        }
        if (providerAdapter.isProvider(dependency)) {
            dependencyService = createReferenceDependencyService(dependencyRequest, () -> new ProviderStrategyFactory(providerAdapter));
            if (dependencyService != null) {
                graphCache.put(dependency, dependencyService, context.errors());
                return dependencyService
                        .service(dependencyRequest, context.errors())
                        .provisionStrategy();
            } else {
                // support empty multi-bind request
                // TODO should not know about MultiBind here -> should be included in above mentioned DependencyMetadata
                if (Arrays.stream(dependency.qualifier().qualifiers()).anyMatch(q -> q instanceof MultiBind)) {
                    return (internalProvider, resolutionContext) ->
                            providerAdapter.provider(ArrayList::new);
                }
            }
        }
        if (dependency.typeKey().raw() == Ref.class) {
            dependencyService = createReferenceDependencyService(dependencyRequest, RefStrategyFactory::new);
            if (dependencyService != null) {
                graphCache.put(dependency, dependencyService, context.errors());
                return dependencyService
                        .service(dependencyRequest, context.errors())
                        .provisionStrategy();
            }  else {
                // support empty multi-bind request
                // TODO should not know about MultiBind here -> should be included in above mentioned DependencyMetadata
                if (Arrays.stream(dependency.qualifier().qualifiers()).anyMatch(q -> q instanceof MultiBind)) {
                    return (internalProvider, resolutionContext) -> (Ref<Object>) ArrayList::new;
                }
            }
        }

        // support empty multi-bind request
        // TODO should not know about MultiBind here -> should be included in above mentioned DependencyMetadata
        if (Arrays.stream(dependency.qualifier().qualifiers()).anyMatch(q -> q instanceof MultiBind)) {
            return (internalProvider, resolutionContext) -> new ArrayList<>();
        }

        return null;
    }

    private DependencyService createReferenceDependencyService(
            final DependencyRequest refRequest, Provider<ReferenceStrategyFactory> factoryProvider) {
        Dependency providerDependency = refRequest.dependency();
        Type providedType = ((ParameterizedType) providerDependency.typeKey().type()).getActualTypeArguments()[0];
        final Dependency provisionDependency = Dependency.from(providerDependency.qualifier(), providedType);
        DependencyService provisionDependencyService = graphCache.get(provisionDependency);
        if (provisionDependencyService == null) {
             return null;
        }
        ProvisionStrategy provisionStrategy = provisionDependencyService.force();
        ReferenceStrategyFactory strategyFactory = factoryProvider.get();
        final ProvisionStrategy providerStrategy = strategyFactory.create(provisionStrategy, this);
        return dependencyServiceFactory.createForReference(
                refRequest, provisionDependencyService, provisionDependency, providerStrategy, strategyFactory);
    }

    private DependencyService createConversionDependencyService(
            Converter converter, Class<?> fromType, Dependency to) {
        Dependency from = Dependency.from(to.qualifier(), fromType);
        DependencyService fromDependencyService = graphCache.get(from);
        if (fromDependencyService != null) {
            return dependencyServiceFactory.createForConversion(fromDependencyService, converter, from, to);
        }
        return null;
    }

}
