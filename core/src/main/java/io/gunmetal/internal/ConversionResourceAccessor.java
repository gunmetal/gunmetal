package io.gunmetal.internal;

import io.gunmetal.spi.Converter;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ProvisionStrategy;

import java.util.Collections;

/**
 * @author rees.byars
 */
class ConversionResourceAccessor implements ResourceAccessor {

    private final ResourceAccessor fromAccessor;
    private final Converter converter;
    private final Dependency fromDependency;
    private final Dependency toDependency;
    private final Binding binding;

    ConversionResourceAccessor(
            ResourceAccessor fromAccessor,
            Converter converter,
            Dependency fromDependency,
            Dependency toDependency) {
        this.fromAccessor = fromAccessor;
        this.converter = converter;
        this.fromDependency = fromDependency;
        this.toDependency = toDependency;
        binding = new BindingImpl(
                fromAccessor.binding().resource(),
                Collections.singletonList(toDependency));
    }

    @Override public ResourceAccessor replicateWith(ComponentContext context) {
        return new ConversionResourceAccessor(
                fromAccessor.replicateWith(context),
                converter,
                fromDependency,
                toDependency);
    }

    @Override public Binding binding() {
        return binding;
    }

    @Override public ProvisionStrategy process(DependencyRequest dependencyRequest, Errors errors) {
        fromAccessor.process(DependencyRequest.create(dependencyRequest, fromDependency), errors);
        return force();
    }

    @Override public ProvisionStrategy force() {
        return (supplier, resolutionContext) ->
                converter.convert(
                        fromAccessor.force().get(supplier, resolutionContext));
    }
}
