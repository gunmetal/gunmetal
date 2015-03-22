package io.gunmetal.internal;

import io.gunmetal.Provider;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ProvisionStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
class CollectionBinding implements Binding {

    private final List<Binding> bindings = new ArrayList<>();
    private final Dependency dependency;
    private final Dependency subDependency;
    private final Provider<Collection<Object>> collectionProvider;

    CollectionBinding(Dependency dependency,
                      Dependency subDependency,
                      Provider<Collection<Object>> collectionProvider) {
        this.dependency = dependency;
        this.subDependency = subDependency;
        this.collectionProvider = collectionProvider;
    }

    @Override public List<Dependency> targets() {
        return Collections.singletonList(dependency);
    }

    @Override public List<Dependency> dependencies() {
        List<Dependency> dependencies = new LinkedList<>();
        bindings.stream().forEach(binding -> dependencies.addAll(binding.dependencies()));
        return dependencies;
    }

    @Override public DependencyResponse service(
            final DependencyRequest dependencyRequest, Errors errors) {
        return () -> {
            DependencyRequest subRequest =
                    DependencyRequest.create(dependencyRequest, subDependency);
            for (Binding binding : bindings) {
                binding.service(subRequest, errors);
            }
            return force();
        };
    }

    @Override public ProvisionStrategy force() {
        return (internalProvider, resolutionContext) -> {
            Collection<Object> collection = collectionProvider.get();
            for (Binding binding : bindings) {
                ProvisionStrategy provisionStrategy = binding.force();
                collection.add(provisionStrategy.get(internalProvider, resolutionContext));
            }
            return collection;
        };
    }

    @Override public boolean isModule() {
        return false;
    }

    @Override public boolean isCollectionElement() {
        return false;
    }

    @Override public boolean allowBindingOverride() {
        return false;
    }

    @Override public Binding replicateWith(GraphContext context) {
        CollectionBinding newBinding =
                new CollectionBinding(dependency, subDependency, collectionProvider);
        for (Binding binding : bindings) {
            newBinding.bindings.add(binding.replicateWith(context));
        }
        return newBinding;
    }

    void add(Binding subBinding) {
        bindings.add(subBinding);
    }

}
