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
import java.util.LinkedList;
import java.util.Queue;

/**
* @author rees.byars
*/
class GraphProvider implements InternalProvider {

    private final ProviderAdapter providerAdapter;
    private final HandlerFactory handlerFactory;
    private final GraphCache graphCache;
    private final GraphContext context;
    private final boolean requireInterfaces;
    private final Queue<Dependency<?>> dependencies = new LinkedList<>();

    GraphProvider(ProviderAdapter providerAdapter,
                  HandlerFactory handlerFactory,
                  GraphCache graphCache,
                  GraphContext context,
                  boolean requireInterfaces) {
        this.providerAdapter = providerAdapter;
        this.handlerFactory = handlerFactory;
        this.graphCache = graphCache;
        this.context = context;
        this.requireInterfaces = requireInterfaces;
    }

    @Override public <T> ProvisionStrategy<? extends T> getProvisionStrategy(final DependencyRequest<T> dependencyRequest) {

        final Dependency<T> dependency = dependencyRequest.dependency();

        if (dependencies.contains(dependency)) {
            // circular dependency request, make "lazy"
            return (p, c) -> getProvisionStrategy(dependencyRequest).get(p, c);
        }

        dependencies.add(dependency);
        if (requireInterfaces &&
                !(dependency.typeKey().raw().isInterface()
                        || dependencyRequest.sourceProvision().overrides().allowNonInterface())) {
            context.errors().add(
                    dependencyRequest.sourceProvision(),
                    "Dependency is not an interface -> " + dependency);
        }
        DependencyRequestHandler<? extends T> requestHandler = graphCache.get(dependency);
        if (requestHandler != null) {
            dependencies.remove();
            return requestHandler
                    .handle(dependencyRequest)
                    .provisionStrategy();
        }
        if (providerAdapter.isProvider(dependency)) {
            requestHandler = createReferenceHandler(dependencyRequest, () -> new ProviderStrategyFactory(providerAdapter));
            if (requestHandler != null) {
                dependencies.remove();
                graphCache.put(dependency, requestHandler, context.errors());
                return requestHandler
                        .handle(dependencyRequest)
                        .provisionStrategy();
            }
        }
        if (dependency.typeKey().raw() == Ref.class) {
            requestHandler = createReferenceHandler(dependencyRequest, RefStrategyFactory::new);
            if (requestHandler != null) {
                dependencies.remove();
                graphCache.put(dependency, requestHandler, context.errors());
                return requestHandler
                        .handle(dependencyRequest)
                        .provisionStrategy();
            }
        }

        requestHandler = handlerFactory.attemptToCreateHandlerFor(dependencyRequest, context);
        if (requestHandler != null) {
            dependencies.remove();
            graphCache.put(dependency, requestHandler, context.errors());
            return requestHandler
                    .handle(dependencyRequest)
                    .provisionStrategy();
        }

        context.errors().add(
                dependencyRequest.sourceProvision(),
                "There is no provider defined for a dependency -> " + dependency);

        // TODO shouldn't need to cast
        return (p, c) -> { ((GraphErrors) context.errors()).throwIfNotEmpty(); return null; };
    }

    private <T, C> DependencyRequestHandler<T> createReferenceHandler(
            final DependencyRequest<T> refRequest, Provider<ReferenceStrategyFactory> factoryProvider) {
        Dependency<?> providerDependency = refRequest.dependency();
        Type providedType = ((ParameterizedType) providerDependency.typeKey().type()).getActualTypeArguments()[0];
        final Dependency<C> provisionDependency = Dependency.from(providerDependency.qualifier(), providedType);
        final DependencyRequestHandler<? extends C> provisionHandler = graphCache.get(provisionDependency);
        if (provisionHandler == null) {
            return null;
        }
        ProvisionStrategy<? extends C> provisionStrategy = provisionHandler.force();
        final ProvisionStrategy<T> providerStrategy = factoryProvider.get().create(provisionStrategy, this);
        return new ReferenceRequestHandler<>(
                refRequest,
                providerStrategy,
                factoryProvider.get(),
                provisionHandler,
                provisionDependency);
    }

}
