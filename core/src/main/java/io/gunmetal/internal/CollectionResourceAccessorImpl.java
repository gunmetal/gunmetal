package io.gunmetal.internal;

import io.gunmetal.Module;
import io.gunmetal.Overrides;
import io.gunmetal.Supplies;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResourceMetadata;
import io.gunmetal.spi.Scopes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author rees.byars
 */
class CollectionResourceAccessorImpl implements CollectionResourceAccessor {

    private final List<ResourceAccessor> elementAccessors = new ArrayList<>();
    private final Supplier<Collection<Object>> collectionSupplier;
    private final Dependency collectionDependency;
    private final Dependency collectionElementDependency;
    private final CollectionBinding binding = new CollectionBinding();
    private final CollectionResource resource = new CollectionResource();
    private final ResourceMetadata<Class<?>> resourceMetadata;

    CollectionResourceAccessorImpl(
            Supplier<Collection<Object>> collectionSupplier,
            Dependency collectionDependency,
            Dependency collectionElementDependency) {
        this.collectionSupplier = collectionSupplier;
        this.collectionDependency = collectionDependency;
        this.collectionElementDependency = collectionElementDependency;
        resourceMetadata = new ResourceMetadata<>(
                        CollectionResourceAccessorImpl.class,
                        CollectionResourceAccessorImpl.class,
                        new ModuleMetadata(
                                CollectionResourceAccessorImpl.class,
                                collectionDependency.qualifier(),
                                Module.NONE),
                        collectionDependency.qualifier(),
                        Scopes.PROTOTYPE,
                        Overrides.NONE,
                        false,
                        false,
                        false,
                        false,
                        Supplies.NONE,
                        false);
    }

    @Override public ResourceAccessor replicateWith(ComponentContext context) {
        CollectionResourceAccessorImpl newAccessor =
                new CollectionResourceAccessorImpl(
                        collectionSupplier,
                        collectionDependency,
                        collectionElementDependency);
        newAccessor.elementAccessors
                .addAll(elementAccessors
                        .stream()
                        .map(element -> element.replicateWith(context))
                        .collect(Collectors.toList()));
        return newAccessor;
    }

    @Override public void add(ResourceAccessor resourceAccessor) {
        elementAccessors.add(resourceAccessor);
    }

    @Override public Binding binding() {
        return binding;
    }

    @Override public ProvisionStrategy process(DependencyRequest dependencyRequest, Errors errors) {
        DependencyRequest subRequest =
                DependencyRequest.create(dependencyRequest, collectionElementDependency);
        for (ResourceAccessor element : elementAccessors) {
            element.process(subRequest, errors);
        }
        return force();
    }

    @Override public ProvisionStrategy force() {
        return resource.provisionStrategy();
    }

    private class CollectionBinding implements Binding {

        @Override public Binding replicateWith(ComponentContext context) {
            throw new UnsupportedOperationException(); // TODO message
        }

        @Override public List<Dependency> targets() {
            return Collections.singletonList(collectionDependency);
        }

        @Override public Resource resource() {
            return resource;
        }

    }

    private class CollectionResource implements Resource {

        @Override public Resource replicateWith(ComponentContext context) {
            throw new UnsupportedOperationException(); // TODO message
        }

        @Override public ResourceMetadata<?> metadata() {
            return resourceMetadata;
        }

        @Override public ProvisionStrategy provisionStrategy() {
            return (supplier, resolutionContext) -> {
                Collection<Object> collection = collectionSupplier.get();
                for (ResourceAccessor element : elementAccessors) {
                    ProvisionStrategy provisionStrategy = element.force();
                    collection.add(provisionStrategy.get(supplier, resolutionContext));
                }
                return collection;
            };
        }

        @Override public List<Dependency> dependencies() {
            return elementAccessors
                    .stream()
                    .flatMap(element -> element.binding().resource().dependencies().stream())
                    .collect(Collectors.toList());
        }

    }
}
