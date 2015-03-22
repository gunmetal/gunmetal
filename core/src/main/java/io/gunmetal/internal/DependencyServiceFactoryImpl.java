package io.gunmetal.internal;

import io.gunmetal.spi.Converter;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.ProvisionStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rees.byars
 */
class DependencyServiceFactoryImpl implements DependencyServiceFactory {

    private final BindingFactory bindingFactory;
    private final RequestVisitorFactory requestVisitorFactory;

    DependencyServiceFactoryImpl(
            BindingFactory bindingFactory,
            RequestVisitorFactory requestVisitorFactory) {
        this.bindingFactory = bindingFactory;
        this.requestVisitorFactory = requestVisitorFactory;
    }

    @Override public List<DependencyService> createForModule(
            Class<?> module, GraphContext context, Set<Class<?>> loadedModules) {
        return bindingFactory.createBindingsForModule(module, context, loadedModules)
                .stream()
                .map(binding -> new DependencyServiceImpl(
                        binding,
                        requestVisitorFactory.resourceRequestVisitor(
                                binding.resource(), context)))
                .collect(Collectors.toList());
    }

    @Override public DependencyService createJit(DependencyRequest dependencyRequest, GraphContext context) {
        Binding binding = bindingFactory.createJitBindingForRequest(dependencyRequest, context);
        if (binding == null) {
            return null;
        }
        return new DependencyServiceImpl(
                binding,
                requestVisitorFactory.resourceRequestVisitor(
                        binding.resource(), context));
    }

    @Override public List<DependencyService> createJitFactoryRequest(DependencyRequest dependencyRequest, GraphContext context) {
        return bindingFactory.createJitFactoryBindingsForRequest(dependencyRequest, context)
                .stream()
                .map(binding -> new DependencyServiceImpl(
                        binding,
                        requestVisitorFactory.resourceRequestVisitor(
                                binding.resource(), context)))
                .collect(Collectors.toList());
    }

    @Override public CollectionDependencyService createForCollection(
            Dependency collectionDependency, Dependency collectionElementDependency) {
        return new CollectionDependencyServiceImpl(
                ArrayList::new,
                collectionDependency,
                collectionElementDependency);
    }

    @Override public DependencyService createForConversion(DependencyService fromService, Converter converter, Dependency fromDependency, Dependency toDependency) {
        return new ConversionDependencyService(
                fromService, converter, fromDependency, toDependency);
    }

    @Override public DependencyService createForReference(
            DependencyRequest referenceRequest,
            DependencyService provisionService,
            Dependency provisionDependency,
            ProvisionStrategy referenceStrategy,
            ReferenceStrategyFactory referenceStrategyFactory) {
        return new ReferenceDependencyService(
                referenceRequest,
                provisionService,
                provisionDependency,
                referenceStrategy,
                referenceStrategyFactory);
    }

}
