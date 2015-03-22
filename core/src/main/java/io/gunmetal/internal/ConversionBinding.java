package io.gunmetal.internal;

import io.gunmetal.spi.Converter;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResolutionContext;

import java.util.Collections;
import java.util.List;

/**
 * @author rees.byars
 */
class ConversionBinding implements Binding {

    private final Binding fromBinding;
    private final Converter converter;
    private final Dependency fromDependency;
    private final Dependency dependency;

    ConversionBinding(
            Binding fromBinding,
            Converter converter,
            Dependency fromDependency,
            Dependency toDependency) {
        this.fromBinding = fromBinding;
        this.converter = converter;
        this.fromDependency = fromDependency;
        this.dependency = toDependency;
    }

    @Override public Binding replicateWith(GraphContext context) {
        return new ConversionBinding(fromBinding.replicateWith(context), converter, fromDependency, dependency);
    }

    @Override public List<Dependency> targets() {
        return Collections.singletonList(dependency);
    }

    @Override public List<Dependency> dependencies() {
        return fromBinding.dependencies();
    }

    @Override public DependencyResponse service(DependencyRequest dependencyRequest, Errors errors) {
        fromBinding.service(DependencyRequest.create(dependencyRequest, fromDependency), errors);
        return this::force;
    }

    @Override public ProvisionStrategy force() {
        return new ProvisionStrategy() {
            @Override public Object get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                return converter.convert(fromBinding.force().get(internalProvider, resolutionContext));
            }
        };
    }

    @Override public boolean isModule() {
        return fromBinding.isModule();
    }

    @Override public boolean isCollectionElement() {
        return fromBinding.isCollectionElement();
    }

    @Override public boolean allowBindingOverride() {
        return fromBinding.allowBindingOverride();
    }

}
