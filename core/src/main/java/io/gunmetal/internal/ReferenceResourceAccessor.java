package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResolutionContext;

import java.util.Collections;

/**
 * @author rees.byars
 */
class ReferenceResourceAccessor implements ResourceAccessor {

    private final DependencyRequest referenceRequest;
    private final ResourceAccessor provisionAccessor;
    private final Dependency provisionDependency;
    private final ProvisionStrategy referenceStrategy;
    private final ReferenceStrategyFactory referenceStrategyFactory;
    private final Binding binding;
    private final ComponentContext componentContext;

    ReferenceResourceAccessor(
            DependencyRequest referenceRequest,
            ResourceAccessor provisionAccessor,
            Dependency provisionDependency,
            ProvisionStrategy referenceStrategy,
            ReferenceStrategyFactory referenceStrategyFactory,
            ComponentContext componentContext) {
        this.referenceRequest = referenceRequest;
        this.provisionAccessor = provisionAccessor;
        this.provisionDependency = provisionDependency;
        this.referenceStrategy = referenceStrategy;
        this.referenceStrategyFactory = referenceStrategyFactory;
        binding = new BindingImpl(
                provisionAccessor.binding().resource(),
                Collections.singletonList(referenceRequest.dependency()));
        this.componentContext = componentContext;
    }

    @Override public ResourceAccessor replicateWith(ComponentContext context) {
        return new ReferenceResourceAccessor(
                referenceRequest,
                provisionAccessor.replicateWith(context),
                provisionDependency,
                new DelegatingProvisionStrategy(context.linkers()),
                referenceStrategyFactory,
                context);
    }

    @Override public Binding binding() {
        return binding;
    }

    @Override public ProvisionStrategy process(DependencyRequest dependencyRequest, Errors errors) {
        provisionAccessor.process(DependencyRequest.create(referenceRequest, provisionDependency), errors);
        return force();
    }

    @Override public ProvisionStrategy force() {
        return referenceStrategy;
    }

    private class DelegatingProvisionStrategy implements ProvisionStrategy {

        ProvisionStrategy delegateStrategy;

        DelegatingProvisionStrategy(Linkers linkers) {
            linkers.addWiringLinker((reference, context) -> {
                ProvisionStrategy provisionStrategy =
                        reference.supply(DependencyRequest.create(referenceRequest, provisionDependency));
                delegateStrategy = referenceStrategyFactory.create(
                        provisionStrategy, reference, componentContext);
            });
        }

        @Override public Object get(DependencySupplier dependencySupplier, ResolutionContext resolutionContext) {
            return delegateStrategy.get(dependencySupplier, resolutionContext);
        }

    }

}
