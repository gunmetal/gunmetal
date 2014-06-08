package io.gunmetal.internal;

import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResolutionContext;

import java.util.Collections;
import java.util.List;

/**
 * @author rees.byars
 */
class ProviderRequestHandler<T, C> implements DependencyRequestHandler<T> {

    private final DependencyRequest<T> providerRequest;
    private final ProvisionStrategy<T> providerStrategy;
    private final ProviderStrategyFactory providerStrategyFactory;
    private final DependencyRequestHandler<? extends C> componentHandler;
    private final Dependency<C> componentDependency;

    ProviderRequestHandler(DependencyRequest<T> providerRequest,
                           ProvisionStrategy<T> providerStrategy,
                           ProviderStrategyFactory providerStrategyFactory,
                           DependencyRequestHandler<? extends C> componentHandler,
                           Dependency<C> componentDependency) {
        this.providerRequest = providerRequest;
        this.providerStrategy = providerStrategy;
        this.providerStrategyFactory = providerStrategyFactory;
        this.componentHandler = componentHandler;
        this.componentDependency = componentDependency;
    }

    @Override public List<Dependency<? super T>> targets() {
        return Collections.<Dependency<? super T>>singletonList(providerRequest.dependency());
    }

    @Override public List<Dependency<?>> dependencies() {
        return componentHandler.dependencies();
    }

    @Override public DependencyResponse<T> handle(DependencyRequest<? super T> dependencyRequest) {
        final DependencyResponse<?> componentResponse =
                componentHandler.handle(DependencyRequest.create(providerRequest, componentDependency));
        return new DependencyResponse<T>() {
            @Override public ValidatedDependencyResponse<T> validateResponse() {
                componentResponse.validateResponse();
                return new ValidatedDependencyResponse<T>() {
                    @Override public ProvisionStrategy<T> getProvisionStrategy() {
                        return providerStrategy;
                    }

                    @Override public ValidatedDependencyResponse<T> validateResponse() {
                        return this;
                    }
                };
            }
        };
    }

    @Override public ProvisionStrategy<T> force() {
        return providerStrategy;
    }

    @Override public ComponentMetadata<?> componentMetadata() {
        return componentHandler.componentMetadata();
    }

    @Override public DependencyRequestHandler<T> newHandlerInstance(Linkers linkers) {
        return new ProviderRequestHandler<>(
                providerRequest,
                new DelegatingProvisionStrategy<T>(linkers),
                providerStrategyFactory,
                componentHandler,
                componentDependency);
    }

    private class DelegatingProvisionStrategy<T> implements ProvisionStrategy<T> {

        ProvisionStrategy<T> delegateStrategy;

        DelegatingProvisionStrategy(Linkers linkers) {
            linkers.addWiringLinker((provider, context) -> {
                ProvisionStrategy<? extends C> componentStrategy =
                        provider.getProvisionStrategy(DependencyRequest.create(providerRequest, componentDependency));
                delegateStrategy = providerStrategyFactory.create(componentStrategy, provider);
            });
        }

        @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
            return delegateStrategy.get(internalProvider, resolutionContext);
        }

    }

}
