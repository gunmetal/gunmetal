package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.Errors;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final Map<Dependency, Binding> bindings = new ConcurrentHashMap<>(64, .75f, 2);
    private final Set<Dependency> overriddenDependencies = Collections.newSetFromMap(new ConcurrentHashMap<>(0));
    private final Queue<Binding> myBindings = new LinkedList<>();

    GraphCache(GraphCache parentCache) {
        this.parentCache = parentCache;
        if (parentCache != null) {
            bindings.putAll(parentCache.bindings);
        }
    }

    void putAll(List<Binding> bindings, Errors errors) {
        for (Binding binding : bindings) {
            putAll(binding, errors);
        }
    }

    void putAll(Binding binding, Errors errors) {
        for (Dependency dependency : binding.targets()) {
            put(dependency, binding, errors);
        }
    }

    void put(final Dependency dependency, Binding binding, Errors errors) {
        if (binding.isCollectionElement()) {
            putCollectionElement(dependency, binding);
        } else {
            Binding previous = bindings.put(dependency, binding);
            if (previous != null) {
                // TODO better messages, include provisions, keep list?
                if (previous.isModule()) { // TODO this is a hack that depends on the order from the binding factory
                    myBindings.add(binding);
                } else if (previous.allowBindingOverride()
                        && binding.allowBindingOverride()) {
                    errors.add("more than one of type with override enabled -> " + dependency);
                    bindings.put(dependency, previous);
                } else if (
                        (overriddenDependencies.contains(dependency)
                                && !binding.allowBindingOverride())
                                || (!previous.allowBindingOverride()
                                && !binding.allowBindingOverride())) {
                    errors.add("more than one of type without override enabled -> " + dependency);
                    bindings.put(dependency, previous);
                } else if (binding.allowBindingOverride()) {
                    myBindings.add(binding);
                    overriddenDependencies.add(dependency);
                } else if (previous.allowBindingOverride()) {
                    bindings.put(dependency, previous);
                    overriddenDependencies.add(dependency);
                }
            } else {
                myBindings.add(binding);
            }
        }
    }

    Binding get(Dependency dependency) {
        return bindings.get(dependency);
    }

    private void putCollectionElement(final Dependency dependency,
                                      Binding binding) {
        Dependency collectionDependency =
                Dependency.from(dependency.qualifier(), new ParameterizedType() {
                    @Override public Type[] getActualTypeArguments() {
                        return new Type[]{dependency.typeKey().type()};
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
        CollectionBinding collectionBinding
                = (CollectionBinding) bindings.get(collectionDependency);
        if (collectionBinding == null) {
            collectionBinding = new CollectionBinding(collectionDependency, dependency, ArrayList::new);
            bindings.put(collectionDependency, collectionBinding);
            myBindings.add(collectionBinding);
        }
        collectionBinding.add(binding);
    }

    @Override public GraphCache replicateWith(GraphContext context) {
        GraphCache newCache = new GraphCache(parentCache);
        for (Binding binding : myBindings) {
            newCache.putAll(binding.replicateWith(context), context.errors());
        }
        return newCache;
    }

}
