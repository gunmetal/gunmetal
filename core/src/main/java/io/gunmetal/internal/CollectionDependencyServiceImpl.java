package io.gunmetal.internal;

import io.gunmetal.Module;
import io.gunmetal.Overrides;
import io.gunmetal.Provider;
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
import java.util.stream.Collectors;

/**
 * @author rees.byars
 */
class CollectionDependencyServiceImpl implements CollectionDependencyService {

    private final List<DependencyService> elementServices = new ArrayList<>();
    private final Provider<Collection<Object>> collectionProvider;
    private final Dependency collectionDependency;
    private final Dependency collectionElementDependency;
    private final CollectionBinding binding = new CollectionBinding();
    private final CollectionResource resource = new CollectionResource();
    private final ResourceMetadata<Class<?>> resourceMetadata;

    CollectionDependencyServiceImpl(
            Provider<Collection<Object>> collectionProvider,
            Dependency collectionDependency,
            Dependency collectionElementDependency) {
        this.collectionProvider = collectionProvider;
        this.collectionDependency = collectionDependency;
        this.collectionElementDependency = collectionElementDependency;
        resourceMetadata = new ResourceMetadata<>(
                        CollectionDependencyServiceImpl.class,
                        CollectionDependencyServiceImpl.class,
                        new ModuleMetadata(
                                CollectionDependencyServiceImpl.class,
                                collectionDependency.qualifier(),
                                Module.NONE),
                        collectionDependency.qualifier(),
                        Scopes.PROTOTYPE,
                        Overrides.NONE,
                        false,
                        false,
                        false,
                        true,
                        false,
                        false);
    }

    @Override public DependencyService replicateWith(GraphContext context) {
        CollectionDependencyServiceImpl newService =
                new CollectionDependencyServiceImpl(
                        collectionProvider,
                        collectionDependency,
                        collectionElementDependency);
        newService.elementServices
                .addAll(elementServices
                        .stream()
                        .map(elementService ->
                                elementService.replicateWith(context))
                        .collect(Collectors.toList()));
        return newService;
    }

    @Override public void add(DependencyService dependencyService) {
        elementServices.add(dependencyService);
    }

    @Override public Binding binding() {
        return binding;
    }

    @Override public DependencyResponse service(DependencyRequest dependencyRequest, Errors errors) {
        return () -> {
            DependencyRequest subRequest =
                    DependencyRequest.create(dependencyRequest, collectionElementDependency);
            for (DependencyService elementService : elementServices) {
                elementService.service(subRequest, errors);
            }
            return force();
        };
    }

    @Override public ProvisionStrategy force() {
        return resource.provisionStrategy();
    }

    private class CollectionBinding implements Binding {

        @Override public Binding replicateWith(GraphContext context) {
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

        @Override public Resource replicateWith(GraphContext context) {
            throw new UnsupportedOperationException(); // TODO message
        }

        @Override public ResourceMetadata<?> metadata() {
            return resourceMetadata;
        }

        @Override public ProvisionStrategy provisionStrategy() {
            return (internalProvider, resolutionContext) -> {
                Collection<Object> collection = collectionProvider.get();
                for (DependencyService elementService : elementServices) {
                    ProvisionStrategy provisionStrategy = elementService.force();
                    collection.add(provisionStrategy.get(internalProvider, resolutionContext));
                }
                return collection;
            };
        }

        @Override public List<Dependency> dependencies() {
            return elementServices
                    .stream()
                    .flatMap(service ->
                            service.binding().resource().dependencies().stream())
                    .collect(Collectors.toList());
        }

    }
}
