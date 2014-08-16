package io.gunmetal.internal;

import io.gunmetal.spi.ProvisionMetadata;
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
    private final DependencyRequestHandler<? extends C> provisionHandler;
    private final Dependency<C> provisionDependency;

    ReferenceRequestHandler(DependencyRequest<T> referenceRequest,
                            ProvisionStrategy<T> referenceStrategy,
                            ReferenceStrategyFactory referenceStrategyFactory,
                            DependencyRequestHandler<? extends C> provisionHandler,
                            Dependency<C> provisionDependency) {
        this.referenceRequest = referenceRequest;
        this.referenceStrategy = referenceStrategy;
        this.referenceStrategyFactory = referenceStrategyFactory;
        this.provisionHandler = provisionHandler;
        this.provisionDependency = provisionDependency;
    }

    @Override public List<Dependency<? super T>> targets() {
        return Collections.<Dependency<? super T>>singletonList(referenceRequest.dependency());
    }

    @Override public List<Dependency<?>> dependencies() {
        return provisionHandler.dependencies();
    }

    @Override public DependencyResponse<T> handle(DependencyRequest<? super T> dependencyRequest) {
        final DependencyResponse<?> provisionResponse =
                provisionHandler.handle(DependencyRequest.create(referenceRequest, provisionDependency));
        return new DependencyResponse<T>() {
            @Override public ValidatedDependencyResponse<T> validateResponse() {
                provisionResponse.validateResponse();
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

    @Override public ProvisionMetadata<?> provisionMetadata() {
        return provisionHandler.provisionMetadata();
    }

    @Override public DependencyRequestHandler<T> replicateWith(GraphContext context) {
        return new ReferenceRequestHandler<>(
                referenceRequest,
                new DelegatingProvisionStrategy<T>(context.linkers()),
                referenceStrategyFactory,
                provisionHandler,
                provisionDependency);
    }

    private class DelegatingProvisionStrategy<T> implements ProvisionStrategy<T> {

        ProvisionStrategy<T> delegateStrategy;

        DelegatingProvisionStrategy(Linkers linkers) {
            linkers.addWiringLinker((reference, context) -> {
                ProvisionStrategy<? extends C> provisionStrategy =
                        reference.getProvisionStrategy(DependencyRequest.create(referenceRequest, provisionDependency));
                delegateStrategy = referenceStrategyFactory.create(provisionStrategy, reference);
            });
        }

        @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
            return delegateStrategy.get(internalProvider, resolutionContext);
        }

    }

}
