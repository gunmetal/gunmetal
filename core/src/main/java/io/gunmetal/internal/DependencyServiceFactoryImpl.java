package io.gunmetal.internal;

import io.gunmetal.spi.Converter;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResourceMetadata;

import java.util.ArrayList;
import java.util.Collections;
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
            Class<?> module, ComponentContext context, Set<Class<?>> loadedModules) {
        return bindingFactory.createBindingsForModule(module, context, loadedModules)
                .stream()
                .map(binding -> new DependencyServiceImpl(
                        binding,
                        requestVisitorFactory.resourceRequestVisitor(
                                binding.resource(), context)))
                .collect(Collectors.toList());
    }

    @Override public DependencyService createJit(DependencyRequest dependencyRequest, ComponentContext context) {
        Binding binding = bindingFactory.createJitBindingForRequest(dependencyRequest, context);
        if (binding == null) {
            return null;
        }
        return new DependencyServiceImpl(
                binding,
                requestVisitorFactory.resourceRequestVisitor(
                        binding.resource(), context));
    }

    @Override public List<DependencyService> createJitFactoryRequest(DependencyRequest dependencyRequest, ComponentContext context) {
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

    @Override public DependencyService createForFalseResource(
            Dependency dependency, ProvisionStrategy provisionStrategy) {

        return new DependencyService() {

            @Override public Binding binding() {

                return new Binding() {

                    @Override public List<Dependency> targets() {
                        return Collections.singletonList(dependency);
                    }

                    @Override public Resource resource() {

                        return new Resource() {

                            @Override public ResourceMetadata<?> metadata() {
                                return null;
                            }

                            @Override public ProvisionStrategy provisionStrategy() {
                                return provisionStrategy;
                            }

                            @Override public List<Dependency> dependencies() {
                                return Collections.emptyList();
                            }

                            @Override public Resource replicateWith(ComponentContext context) {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }

                    @Override public Binding replicateWith(ComponentContext context) {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override public DependencyResponse service(DependencyRequest dependencyRequest, Errors errors) {
                return () -> provisionStrategy;
            }

            @Override public ProvisionStrategy force() {
                return provisionStrategy;
            }

            @Override public DependencyService replicateWith(ComponentContext context) {
                return createForFalseResource(dependency, provisionStrategy);
            }

        };
    }

}
