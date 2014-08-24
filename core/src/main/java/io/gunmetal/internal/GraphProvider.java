package io.gunmetal.internal;

import io.gunmetal.Provider;
import io.gunmetal.Ref;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProviderAdapter;
import io.gunmetal.spi.ProvisionStrategy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 *
 * Instances of the provider should not be accessed concurrently.
 *
* @author rees.byars
*/
class GraphProvider implements InternalProvider {

    private final ProviderAdapter providerAdapter;
    private final ResourceProxyFactory resourceProxyFactory;
    private final GraphCache graphCache;
    private final GraphContext context;
    private final boolean requireInterfaces;

    GraphProvider(ProviderAdapter providerAdapter,
                  ResourceProxyFactory resourceProxyFactory,
                  GraphCache graphCache,
                  GraphContext context,
                  boolean requireInterfaces) {
        this.providerAdapter = providerAdapter;
        this.resourceProxyFactory = resourceProxyFactory;
        this.graphCache = graphCache;
        this.context = context;
        this.requireInterfaces = requireInterfaces;
    }

    @Override public synchronized <T> ProvisionStrategy<? extends T> getProvisionStrategy(
            final DependencyRequest<T> dependencyRequest) {

        final Dependency<T> dependency = dependencyRequest.dependency();

        if (requireInterfaces &&
                !(dependency.typeKey().raw().isInterface()
                        || dependencyRequest.sourceProvision().overrides().allowNonInterface())) {
            context.errors().add(
                    dependencyRequest.sourceProvision(),
                    "Dependency is not an interface -> " + dependency);
        }
        ResourceProxy<? extends T> resourceProxy = graphCache.get(dependency);
        if (resourceProxy != null) {
            return resourceProxy
                    .service(dependencyRequest)
                    .provisionStrategy();
        }
        if (providerAdapter.isProvider(dependency)) {
            resourceProxy = createReferenceProxy(dependencyRequest, () -> new ProviderStrategyFactory(providerAdapter));
            if (resourceProxy != null) {
                graphCache.put(dependency, resourceProxy, context.errors());
                return resourceProxy
                        .service(dependencyRequest)
                        .provisionStrategy();
            }
        }
        if (dependency.typeKey().raw() == Ref.class) {
            resourceProxy = createReferenceProxy(dependencyRequest, RefStrategyFactory::new);
            if (resourceProxy != null) {
                graphCache.put(dependency, resourceProxy, context.errors());
                return resourceProxy
                        .service(dependencyRequest)
                        .provisionStrategy();
            }
        }

        resourceProxy = resourceProxyFactory.createJitProxyForRequest(dependencyRequest, context);
        if (resourceProxy != null) {
            graphCache.put(dependency, resourceProxy, context.errors());
            return resourceProxy
                    .service(dependencyRequest)
                    .provisionStrategy();
        }

        //dependencyStack.pop();
        context.errors().add(
                dependencyRequest.sourceProvision(),
                "There is no provider defined for a dependency -> " + dependency);

        // TODO shouldn't need to cast
        return (p, c) -> { ((GraphErrors) context.errors()).throwIfNotEmpty(); return null; };

    }

    private <T, C> ResourceProxy<T> createReferenceProxy(
            final DependencyRequest<T> refRequest, Provider<ReferenceStrategyFactory> factoryProvider) {
        Dependency<?> providerDependency = refRequest.dependency();
        Type providedType = ((ParameterizedType) providerDependency.typeKey().type()).getActualTypeArguments()[0];
        final Dependency<C> provisionDependency = Dependency.from(providerDependency.qualifier(), providedType);
        final ResourceProxy<? extends C> provisionProxy = graphCache.get(provisionDependency);
        if (provisionProxy == null) {
            return null;
        }
        ProvisionStrategy<? extends C> provisionStrategy = provisionProxy.force();
        final ProvisionStrategy<T> providerStrategy = factoryProvider.get().create(provisionStrategy, this);
        return new ReferenceResourceProxy<>(
                refRequest,
                providerStrategy,
                factoryProvider.get(),
                provisionProxy,
                provisionDependency);
    }

}
