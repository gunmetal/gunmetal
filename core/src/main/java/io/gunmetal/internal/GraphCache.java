package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ResourceMetadata;
import io.gunmetal.util.Generics;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
* @author rees.byars
*/
class GraphCache implements Replicable<GraphCache> {

    private final GraphCache parentCache;
    private final Map<Dependency<?>, Binding<?>> bindings = new ConcurrentHashMap<>(64, .75f, 2);
    private final Set<Dependency<?>> overriddenDependencies = Collections.newSetFromMap(new ConcurrentHashMap<>(0));
    private final Queue<Binding<?>> myBindings = new LinkedList<>();

    GraphCache(GraphCache parentCache) {
        this.parentCache = parentCache;
        if (parentCache != null) {
            bindings.putAll(parentCache.bindings);
        }
    }

    void putAll(List<Binding<?>> bindings, Errors errors) {
        for (Binding<?> binding : bindings) {
            putAll(binding, errors);
        }
    }

    <T> void putAll(Binding<T> binding, Errors errors) {
        for (Dependency<? super T> dependency : binding.targets()) {
            put(dependency, binding, errors);
        }
    }

    <T> void put(final Dependency<? super T> dependency, Binding<T> binding, Errors errors) {
        ResourceMetadata<?> currentProvision = binding.resourceMetadata();
        if (currentProvision.isCollectionElement()) {
            putCollectionElement(dependency, binding);
        } else {
            Binding<?> previous = bindings.put(dependency, binding);
            if (previous != null) {
                ResourceMetadata<?> previousProvision = previous.resourceMetadata();
                // TODO better messages, include provisions, keep list?
                if (previousProvision.isModule()) { // TODO this is a hack that depends on the order from the binding factory
                    myBindings.add(binding);
                } else if (previousProvision.overrides().allowMappingOverride()
                        && currentProvision.overrides().allowMappingOverride()) {
                    errors.add("more than one of type with override enabled -> " + dependency);
                    bindings.put(dependency, previous);
                } else if (
                        (overriddenDependencies.contains(dependency)
                                && !currentProvision.overrides().allowMappingOverride())
                        || (!previousProvision.overrides().allowMappingOverride()
                                && !currentProvision.overrides().allowMappingOverride())) {
                    errors.add("more than one of type without override enabled -> " + dependency);
                    bindings.put(dependency, previous);
                } else if (currentProvision.overrides().allowMappingOverride()) {
                    myBindings.add(binding);
                    overriddenDependencies.add(dependency);
                } else if (previousProvision.overrides().allowMappingOverride()) {
                    bindings.put(dependency, previous);
                    overriddenDependencies.add(dependency);
                }
            } else {
                myBindings.add(binding);
            }
        }
    }

    <T> Binding<? extends T> get(Dependency<T> dependency) {
        return Generics.as(bindings.get(dependency));
    }

    private <T> void putCollectionElement(final Dependency<T> dependency,
                                  Binding<? extends T> binding) {
        Dependency<Collection<T>> collectionDependency =
                Dependency.from(dependency.qualifier(), new ParameterizedType() {
                    @Override public Type[] getActualTypeArguments() {
                        return new Type[] {dependency.typeKey().type()};
                    }

                    @Override public Type getRawType() {
                        return List.class;
                    }

                    @Override public Type getOwnerType() {
                        return null;
                    }

                    @Override public int hashCode() {
                        return Arrays.hashCode(getActualTypeArguments()) * 67 + getRawType().hashCode();
                    }

                    @Override public boolean equals(Object target) {
                        if (target == this) {
                            return true;
                        }
                        if (!(target instanceof ParameterizedType)) {
                            return false;
                        }
                        ParameterizedType parameterizedType = (ParameterizedType) target;
                        return parameterizedType.getRawType().equals(getRawType())
                                && Arrays.equals(parameterizedType.getActualTypeArguments(), getActualTypeArguments());
                    }

                });
        CollectionBinding<T> collectionBinding
                = Generics.as(bindings.get(collectionDependency));
        if (collectionBinding == null) {
            collectionBinding = new CollectionBinding<>(collectionDependency, dependency, ArrayList::new);
            bindings.put(collectionDependency, collectionBinding);
            myBindings.add(collectionBinding);
        }
        collectionBinding.add(binding);
    }

    @Override public GraphCache replicateWith(GraphContext context) {
        GraphCache newCache = new GraphCache(parentCache);
        for (Binding<?> binding : myBindings) {
            newCache.putAll(binding.replicateWith(context), context.errors());
        }
        return newCache;
    }

}
