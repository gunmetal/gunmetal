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
* @author rees.byars
*/
class InternalProviderImpl implements InternalProvider {

    private final ProviderAdapter providerAdapter;
    private final HandlerFactory handlerFactory;
    private final HandlerCache handlerCache;
    private final GraphContext context;

    InternalProviderImpl(ProviderAdapter providerAdapter,
                         HandlerFactory handlerFactory,
                         HandlerCache handlerCache,
                         GraphContext context) {
        this.providerAdapter = providerAdapter;
        this.handlerFactory = handlerFactory;
        this.handlerCache = handlerCache;
        this.context = context;
    }

    @Override public <T> ProvisionStrategy<? extends T> getProvisionStrategy(final DependencyRequest<T> dependencyRequest) {
        final Dependency<T> dependency = dependencyRequest.dependency();
        DependencyRequestHandler<? extends T> requestHandler = handlerCache.get(dependency);
        if (requestHandler != null) {
            return requestHandler
                    .handle(dependencyRequest)
                    .validateResponse()
                    .getProvisionStrategy();
        }
        if (providerAdapter.isProvider(dependency)) {
            requestHandler = createReferenceHandler(dependencyRequest, () -> new ProviderStrategyFactory(providerAdapter));
            if (requestHandler != null) {
                handlerCache.put(dependency, requestHandler);
                return requestHandler
                        .handle(dependencyRequest)
                        .validateResponse()
                        .getProvisionStrategy();
            }
        }
        if (dependency.typeKey().raw() == Ref.class) {
            requestHandler = createReferenceHandler(dependencyRequest, RefStrategyFactory::new);
            if (requestHandler != null) {
                handlerCache.put(dependency, requestHandler);
                return requestHandler
                        .handle(dependencyRequest)
                        .validateResponse()
                        .getProvisionStrategy();
            }
        }
        requestHandler = handlerFactory.attemptToCreateHandlerFor(dependencyRequest, context);
        if (requestHandler != null) {
            handlerCache.put(dependency, requestHandler);
            return requestHandler
                    .handle(dependencyRequest)
                    .validateResponse()
                    .getProvisionStrategy();
        }
        throw new DependencyException("missing " + dependency); // TODO
    }

    private <T, C> DependencyRequestHandler<T> createReferenceHandler(
            final DependencyRequest<T> refRequest, Provider<ReferenceStrategyFactory> factoryProvider) {
        Dependency<?> providerDependency = refRequest.dependency();
        Type providedType = ((ParameterizedType) providerDependency.typeKey().type()).getActualTypeArguments()[0];
        final Dependency<C> componentDependency = Dependency.from(providerDependency.qualifier(), providedType);
        final DependencyRequestHandler<? extends C> componentHandler = handlerCache.get(componentDependency);
        if (componentHandler == null) {
            return null;
        }
        ProvisionStrategy<? extends C> componentStrategy = componentHandler.force();
        final ProvisionStrategy<T> providerStrategy = factoryProvider.get().create(componentStrategy, this);
        return new ReferenceRequestHandler<>(
                refRequest,
                providerStrategy,
                factoryProvider.get(),
                componentHandler,
                componentDependency);
    }

}
