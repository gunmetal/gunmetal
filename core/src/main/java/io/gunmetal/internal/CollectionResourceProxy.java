package io.gunmetal.internal;

import io.gunmetal.Module;
import io.gunmetal.Overrides;
import io.gunmetal.Provider;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ResourceMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.Scopes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
* @author rees.byars
*/
class CollectionResourceProxy<T> implements ResourceProxy<Collection<T>> {

    private final List<ResourceProxy<? extends T>> resourceProxies = new ArrayList<>();
    private final Dependency<Collection<T>> dependency;
    private final Dependency<T> subDependency;
    private final Provider<Collection<T>> collectionProvider;

    CollectionResourceProxy(Dependency<Collection<T>> dependency,
                            Dependency<T> subDependency,
                            Provider<Collection<T>> collectionProvider) {
        this.dependency = dependency;
        this.subDependency = subDependency;
        this.collectionProvider = collectionProvider;
    }

    @Override public List<Dependency<? super Collection<T>>> targets() {
        return Collections.<Dependency<? super Collection<T>>>singletonList(dependency);
    }

    @Override public List<Dependency<?>> dependencies() {
        List<Dependency<?>> dependencies = new LinkedList<>();
        resourceProxies.stream().forEach(resourceProxy -> dependencies.addAll(resourceProxy.dependencies()));
        return dependencies;
    }

    @Override public DependencyResponse<Collection<T>> service(
            final DependencyRequest<? super Collection<T>> dependencyRequest) {
        return () -> {
            DependencyRequest<T> subRequest =
                    DependencyRequest.create(dependencyRequest, subDependency);
            for (ResourceProxy<? extends T> resourceProxy : resourceProxies) {
                resourceProxy.service(subRequest);
            }
            return force();
        };
    }

    @Override public ProvisionStrategy<Collection<T>> force() {
        return (internalProvider, resolutionContext) -> {
            Collection<T> collection = collectionProvider.get();
            for (ResourceProxy<? extends T> resourceProxy : resourceProxies) {
                ProvisionStrategy<? extends T> provisionStrategy = resourceProxy.force();
                collection.add(provisionStrategy.get(internalProvider, resolutionContext));
            }
            return collection;
        };
    }

    @Override public ResourceMetadata<?> resourceMetadata() {
        return new ResourceMetadata<Class<?>>(
                CollectionResourceProxy.class,
                CollectionResourceProxy.class,
                new ModuleMetadata(CollectionResourceProxy.class, Qualifier.NONE, Module.NONE),
                dependency.qualifier(),
                Scopes.PROTOTYPE,
                Overrides.NONE,
                false,
                false,
                false,
                false,
                true);
    }

    @Override public ResourceProxy<Collection<T>> replicateWith(GraphContext context) {
        CollectionResourceProxy<T> newProxy =
                new CollectionResourceProxy<>(dependency, subDependency, collectionProvider);
        for (ResourceProxy<? extends T> resourceProxy : resourceProxies) {
            newProxy.resourceProxies.add(resourceProxy.replicateWith(context));
        }
        return newProxy;
    }

    void add(ResourceProxy<? extends T> subProxy) {
        resourceProxies.add(subProxy);
    }

}
