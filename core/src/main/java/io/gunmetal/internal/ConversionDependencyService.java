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
class ConversionDependencyService implements DependencyService {

    private final DependencyService fromService;
    private final Converter converter;
    private final Dependency fromDependency;
    private final Dependency toDependency;
    private final Binding binding;

    ConversionDependencyService(
            DependencyService fromService,
            Converter converter,
            Dependency fromDependency,
            Dependency toDependency) {
        this.fromService = fromService;
        this.converter = converter;
        this.fromDependency = fromDependency;
        this.toDependency = toDependency;
        binding = new BindingImpl(
                fromService.binding().resource(),
                Collections.singletonList(toDependency));
    }

    @Override public DependencyService replicateWith(GraphContext context) {
        return new ConversionDependencyService(
                fromService.replicateWith(context),
                converter,
                fromDependency,
                toDependency);
    }

    @Override public Binding binding() {
        return binding;
    }

    @Override public DependencyResponse service(DependencyRequest dependencyRequest, Errors errors) {
        fromService.service(DependencyRequest.create(dependencyRequest, fromDependency), errors);
        return this::force;
    }

    @Override public ProvisionStrategy force() {
        return (internalProvider, resolutionContext) ->
                converter.convert(
                        fromService.force().get(internalProvider, resolutionContext));
    }
}
