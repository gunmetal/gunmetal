package io.gunmetal.internal;

import io.gunmetal.spi.ResourceMetadata;
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
class ReferenceResourceProxy<T, C> implements ResourceProxy<T> {

    private final DependencyRequest<T> referenceRequest;
    private final ProvisionStrategy<T> referenceStrategy;
    private final ReferenceStrategyFactory referenceStrategyFactory;
    private final ResourceProxy<? extends C> provisionProxy;
    private final Dependency<C> provisionDependency;

    ReferenceResourceProxy(DependencyRequest<T> referenceRequest,
                           ProvisionStrategy<T> referenceStrategy,
                           ReferenceStrategyFactory referenceStrategyFactory,
                           ResourceProxy<? extends C> provisionProxy,
                           Dependency<C> provisionDependency) {
        this.referenceRequest = referenceRequest;
        this.referenceStrategy = referenceStrategy;
        this.referenceStrategyFactory = referenceStrategyFactory;
        this.provisionProxy = provisionProxy;
        this.provisionDependency = provisionDependency;
    }

    @Override public List<Dependency<? super T>> targets() {
        return Collections.<Dependency<? super T>>singletonList(referenceRequest.dependency());
    }

    @Override public List<Dependency<?>> dependencies() {
        return provisionProxy.dependencies();
    }

    @Override public DependencyResponse<T> service(DependencyRequest<? super T> dependencyRequest) {
        provisionProxy.service(DependencyRequest.create(referenceRequest, provisionDependency));
        return () -> referenceStrategy;
    }

    @Override public ProvisionStrategy<T> force() {
        return referenceStrategy;
    }

    @Override public ResourceMetadata<?> resourceMetadata() {
        return provisionProxy.resourceMetadata();
    }

    @Override public ResourceProxy<T> replicateWith(GraphContext context) {
        return new ReferenceResourceProxy<>(
                referenceRequest,
                new DelegatingProvisionStrategy<T>(context.linkers()),
                referenceStrategyFactory,
                provisionProxy,
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
