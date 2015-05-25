package io.gunmetal.internal;

import io.gunmetal.spi.Converter;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResourceMetadata;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rees.byars
 */
class ResourceAccessorFactoryImpl implements ResourceAccessorFactory {

    private final BindingFactory bindingFactory;
    private final RequestVisitorFactory requestVisitorFactory;

    ResourceAccessorFactoryImpl(
            BindingFactory bindingFactory,
            RequestVisitorFactory requestVisitorFactory) {
        this.bindingFactory = bindingFactory;
        this.requestVisitorFactory = requestVisitorFactory;
    }

    @Override public List<ResourceAccessor> createForModule(
            Class<?> module, ComponentContext context) {
        return bindingFactory.createBindingsForModule(module, context)
                .stream()
                .map(binding -> new ResourceAccessorImpl(
                        binding,
                        requestVisitorFactory.resourceRequestVisitor(
                                binding.resource(), context)))
                .collect(Collectors.toList());
    }

    @Override public ResourceAccessor createForParam(
            Parameter parameter, ComponentContext context) {
        Binding binding = bindingFactory.createParamBinding(parameter, context);
        return new ResourceAccessorImpl(
                binding,
                requestVisitorFactory.resourceRequestVisitor(
                        binding.resource(), context));
    }

    @Override public ResourceAccessor createJit(DependencyRequest dependencyRequest, ComponentContext context) {
        Binding binding = bindingFactory.createJitBindingForRequest(dependencyRequest, context);
        if (binding == null) {
            return null;
        }
        return new ResourceAccessorImpl(
                binding,
                requestVisitorFactory.resourceRequestVisitor(
                        binding.resource(), context));
    }

    @Override public List<ResourceAccessor> createJitFactoryRequest(DependencyRequest dependencyRequest, ComponentContext context) {
        return bindingFactory.createJitFactoryBindingsForRequest(dependencyRequest, context)
                .stream()
                .map(binding -> new ResourceAccessorImpl(
                        binding,
                        requestVisitorFactory.resourceRequestVisitor(
                                binding.resource(), context)))
                .collect(Collectors.toList());
    }

    @Override public CollectionResourceAccessor createForCollection(
            Dependency collectionDependency, Dependency collectionElementDependency) {
        return new CollectionResourceAccessorImpl(
                ArrayList::new,
                collectionDependency,
                collectionElementDependency);
    }

    @Override public ResourceAccessor createForConversion(
            ResourceAccessor fromAccessor,
            Converter converter,
            Dependency fromDependency,
            Dependency toDependency) {
        return new ConversionResourceAccessor(
                fromAccessor, converter, fromDependency, toDependency);
    }

    @Override public ResourceAccessor createForReference(
            DependencyRequest referenceRequest,
            ResourceAccessor provisionAccessor,
            Dependency provisionDependency,
            ProvisionStrategy referenceStrategy,
            ReferenceStrategyFactory referenceStrategyFactory,
            ComponentContext componentContext) {
        return new ReferenceResourceAccessor(
                referenceRequest,
                provisionAccessor,
                provisionDependency,
                referenceStrategy,
                referenceStrategyFactory,
                componentContext);
    }

    @Override public ResourceAccessor createForFalseResource(
            Dependency dependency, ProvisionStrategy provisionStrategy) {

        // TODO extract to class, better messages
        return new ResourceAccessor() {

            @Override public Binding binding() {

                return new Binding() {

                    @Override public List<Dependency> targets() {
                        return Collections.singletonList(dependency);
                    }

                    @Override public Resource resource() {

                        return new Resource() {

                            @Override public ResourceMetadata<?> metadata() {
                                throw new UnsupportedOperationException();
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

            @Override public ProvisionStrategy process(DependencyRequest dependencyRequest, Errors errors) {
                return provisionStrategy;
            }

            @Override public ProvisionStrategy force() {
                return provisionStrategy;
            }

            @Override public ResourceAccessor replicateWith(ComponentContext context) {
                return createForFalseResource(dependency, provisionStrategy);
            }

        };
    }

}
