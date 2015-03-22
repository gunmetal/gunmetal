package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResolutionContext;

import java.util.Collections;
import java.util.List;

/**
 * @author rees.byars
 */
class ReferenceBinding implements Binding {

    private final DependencyRequest referenceRequest;
    private final ProvisionStrategy referenceStrategy;
    private final ReferenceStrategyFactory referenceStrategyFactory;
    private final Binding provisionBinding;
    private final Dependency provisionDependency;

    ReferenceBinding(DependencyRequest referenceRequest,
                     ProvisionStrategy referenceStrategy,
                     ReferenceStrategyFactory referenceStrategyFactory,
                     Binding provisionBinding,
                     Dependency provisionDependency) {
        this.referenceRequest = referenceRequest;
        this.referenceStrategy = referenceStrategy;
        this.referenceStrategyFactory = referenceStrategyFactory;
        this.provisionBinding = provisionBinding;
        this.provisionDependency = provisionDependency;
    }

    @Override public List<Dependency> targets() {
        return Collections.singletonList(referenceRequest.dependency());
    }

    @Override public List<Dependency> dependencies() {
        return provisionBinding.dependencies();
    }

    @Override public DependencyResponse service(DependencyRequest dependencyRequest,
                                                Errors errors) {
        provisionBinding.service(DependencyRequest.create(referenceRequest, provisionDependency), errors);
        return () -> referenceStrategy;
    }

    @Override public ProvisionStrategy force() {
        return referenceStrategy;
    }

    @Override public boolean isModule() {
        return false;
    }

    @Override public boolean isCollectionElement() {
        return false;
    }

    @Override public boolean allowBindingOverride() {
        return false;
    }

    @Override public Binding replicateWith(GraphContext context) {
        return new ReferenceBinding(
                referenceRequest,
                new DelegatingProvisionStrategy(context.linkers()),
                referenceStrategyFactory,
                provisionBinding,
                provisionDependency);
    }

    private class DelegatingProvisionStrategy implements ProvisionStrategy {

        ProvisionStrategy delegateStrategy;

        DelegatingProvisionStrategy(Linkers linkers) {
            linkers.addWiringLinker((reference, context) -> {
                ProvisionStrategy provisionStrategy =
                        reference.getProvisionStrategy(DependencyRequest.create(referenceRequest, provisionDependency));
                delegateStrategy = referenceStrategyFactory.create(provisionStrategy, reference);
            });
        }

        @Override public Object get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
            return delegateStrategy.get(internalProvider, resolutionContext);
        }

    }

}
