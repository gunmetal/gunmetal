package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.spi.ResourceMetadata;

import java.util.Collections;
import java.util.List;

/**
 * @author rees.byars
 */
class ReferenceBinding<T, C> implements Binding<T> {

    private final DependencyRequest<T> referenceRequest;
    private final ProvisionStrategy<T> referenceStrategy;
    private final ReferenceStrategyFactory referenceStrategyFactory;
    private final Binding<? extends C> provisionBinding;
    private final Dependency<C> provisionDependency;

    ReferenceBinding(DependencyRequest<T> referenceRequest,
                     ProvisionStrategy<T> referenceStrategy,
                     ReferenceStrategyFactory referenceStrategyFactory,
                     Binding<? extends C> provisionBinding,
                     Dependency<C> provisionDependency) {
        this.referenceRequest = referenceRequest;
        this.referenceStrategy = referenceStrategy;
        this.referenceStrategyFactory = referenceStrategyFactory;
        this.provisionBinding = provisionBinding;
        this.provisionDependency = provisionDependency;
    }

    @Override public List<Dependency<? super T>> targets() {
        return Collections.<Dependency<? super T>>singletonList(referenceRequest.dependency());
    }

    @Override public List<Dependency<?>> dependencies() {
        return provisionBinding.dependencies();
    }

    @Override public DependencyResponse<T> service(DependencyRequest<? super T> dependencyRequest,
                                                   Errors errors) {
        provisionBinding.service(DependencyRequest.create(referenceRequest, provisionDependency), errors);
        return () -> referenceStrategy;
    }

    @Override public ProvisionStrategy<T> force() {
        return referenceStrategy;
    }

    @Override public ResourceMetadata<?> resourceMetadata() {
        return provisionBinding.resourceMetadata();
    }

    @Override public Binding<T> replicateWith(GraphContext context) {
        return new ReferenceBinding<>(
                referenceRequest,
                new DelegatingProvisionStrategy<T>(context.linkers()),
                referenceStrategyFactory,
                provisionBinding,
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
