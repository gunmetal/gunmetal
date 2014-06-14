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
class ReferenceRequestHandler<T, C> implements DependencyRequestHandler<T> {

    private final DependencyRequest<T> referenceRequest;
    private final ProvisionStrategy<T> referenceStrategy;
    private final ReferenceStrategyFactory referenceStrategyFactory;
    private final DependencyRequestHandler<? extends C> componentHandler;
    private final Dependency<C> componentDependency;

    ReferenceRequestHandler(DependencyRequest<T> referenceRequest,
                            ProvisionStrategy<T> referenceStrategy,
                            ReferenceStrategyFactory referenceStrategyFactory,
                            DependencyRequestHandler<? extends C> componentHandler,
                            Dependency<C> componentDependency) {
        this.referenceRequest = referenceRequest;
        this.referenceStrategy = referenceStrategy;
        this.referenceStrategyFactory = referenceStrategyFactory;
        this.componentHandler = componentHandler;
        this.componentDependency = componentDependency;
    }

    @Override public List<Dependency<? super T>> targets() {
        return Collections.<Dependency<? super T>>singletonList(referenceRequest.dependency());
    }

    @Override public List<Dependency<?>> dependencies() {
        return componentHandler.dependencies();
    }

    @Override public DependencyResponse<T> handle(DependencyRequest<? super T> dependencyRequest) {
        final DependencyResponse<?> componentResponse =
                componentHandler.handle(DependencyRequest.create(referenceRequest, componentDependency));
        return new DependencyResponse<T>() {
            @Override public ValidatedDependencyResponse<T> validateResponse() {
                componentResponse.validateResponse();
                return new ValidatedDependencyResponse<T>() {
                    @Override public ProvisionStrategy<T> getProvisionStrategy() {
                        return referenceStrategy;
                    }

                    @Override public ValidatedDependencyResponse<T> validateResponse() {
                        return this;
                    }
                };
            }
        };
    }

    @Override public ProvisionStrategy<T> force() {
        return referenceStrategy;
    }

    @Override public ComponentMetadata<?> componentMetadata() {
        return componentHandler.componentMetadata();
    }

    @Override public DependencyRequestHandler<T> replicate(GraphContext context) {
        return new ReferenceRequestHandler<>(
                referenceRequest,
                new DelegatingProvisionStrategy<T>(context.linkers()),
                referenceStrategyFactory,
                componentHandler,
                componentDependency);
    }

    private class DelegatingProvisionStrategy<T> implements ProvisionStrategy<T> {

        ProvisionStrategy<T> delegateStrategy;

        DelegatingProvisionStrategy(Linkers linkers) {
            linkers.addWiringLinker((reference, context) -> {
                ProvisionStrategy<? extends C> componentStrategy =
                        reference.getProvisionStrategy(DependencyRequest.create(referenceRequest, componentDependency));
                delegateStrategy = referenceStrategyFactory.create(componentStrategy, reference);
            });
        }

        @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
            return delegateStrategy.get(internalProvider, resolutionContext);
        }

    }

}
