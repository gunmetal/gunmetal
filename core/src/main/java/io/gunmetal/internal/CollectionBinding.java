package io.gunmetal.internal;

import io.gunmetal.Module;
import io.gunmetal.Overrides;
import io.gunmetal.Provider;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResourceMetadata;
import io.gunmetal.spi.Scopes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
* @author rees.byars
*/
class CollectionBinding<T> implements Binding<Collection<T>> {

    private final List<Binding<? extends T>> bindings = new ArrayList<>();
    private final Dependency<Collection<T>> dependency;
    private final Dependency<T> subDependency;
    private final Provider<Collection<T>> collectionProvider;

    CollectionBinding(Dependency<Collection<T>> dependency,
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
        bindings.stream().forEach(binding -> dependencies.addAll(binding.dependencies()));
        return dependencies;
    }

    @Override public DependencyResponse<Collection<T>> service(
            final DependencyRequest<? super Collection<T>> dependencyRequest, Errors errors) {
        return () -> {
            DependencyRequest<T> subRequest =
                    DependencyRequest.create(dependencyRequest, subDependency);
            for (Binding<? extends T> binding : bindings) {
                binding.service(subRequest, errors);
            }
            return force();
        };
    }

    @Override public ProvisionStrategy<Collection<T>> force() {
        return (internalProvider, resolutionContext) -> {
            Collection<T> collection = collectionProvider.get();
            for (Binding<? extends T> binding : bindings) {
                ProvisionStrategy<? extends T> provisionStrategy = binding.force();
                collection.add(provisionStrategy.get(internalProvider, resolutionContext));
            }
            return collection;
        };
    }

    @Override public ResourceMetadata<?> resourceMetadata() {
        return new ResourceMetadata<Class<?>>(
                CollectionBinding.class,
                CollectionBinding.class,
                new ModuleMetadata(CollectionBinding.class, Qualifier.NONE, Module.NONE),
                dependency.qualifier(),
                Scopes.PROTOTYPE,
                Overrides.NONE,
                false,
                false,
                false,
                false,
                true);
    }

    @Override public Binding<Collection<T>> replicateWith(GraphContext context) {
        CollectionBinding<T> newBinding =
                new CollectionBinding<>(dependency, subDependency, collectionProvider);
        for (Binding<? extends T> binding : bindings) {
            newBinding.bindings.add(binding.replicateWith(context));
        }
        return newBinding;
    }

    void add(Binding<? extends T> subBinding) {
        bindings.add(subBinding);
    }

}
