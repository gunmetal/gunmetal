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
    private final Map<Dependency<?>, ResourceProxy<?>> resourceProxies = new ConcurrentHashMap<>(64, .75f, 2);
    private final Set<Dependency<?>> overriddenDependencies = Collections.newSetFromMap(new ConcurrentHashMap<>(0));
    private final Queue<ResourceProxy<?>> myProxies = new LinkedList<>();

    GraphCache(GraphCache parentCache) {
        this.parentCache = parentCache;
        if (parentCache != null) {
            resourceProxies.putAll(parentCache.resourceProxies);
        }
    }

    void putAll(List<ResourceProxy<?>> resourceProxies, Errors errors) {
        for (ResourceProxy<?> resourceProxy : resourceProxies) {
            putAll(resourceProxy, errors);
        }
    }

    <T> void putAll(ResourceProxy<T> resourceProxy, Errors errors) {
        for (Dependency<? super T> dependency : resourceProxy.targets()) {
            put(dependency, resourceProxy, errors);
        }
    }

    <T> void put(final Dependency<? super T> dependency, ResourceProxy<T> resourceProxy, Errors errors) {
        ResourceMetadata<?> currentProvision = resourceProxy.resourceMetadata();
        if (currentProvision.isCollectionElement()) {
            putCollectionElement(dependency, resourceProxy);
        } else {
            ResourceProxy<?> previous = resourceProxies.put(dependency, resourceProxy);
            if (previous != null) {
                ResourceMetadata<?> previousProvision = previous.resourceMetadata();
                // TODO better messages, include provisions, keep list?
                if (previousProvision.isModule()) { // TODO this is a hack that depends on the order from the proxy factory
                    myProxies.add(resourceProxy);
                } else if (previousProvision.overrides().allowMappingOverride()
                        && currentProvision.overrides().allowMappingOverride()) {
                    errors.add("more than one of type with override enabled -> " + dependency);
                    resourceProxies.put(dependency, previous);
                } else if (
                        (overriddenDependencies.contains(dependency)
                                && !currentProvision.overrides().allowMappingOverride())
                        || (!previousProvision.overrides().allowMappingOverride()
                                && !currentProvision.overrides().allowMappingOverride())) {
                    errors.add("more than one of type without override enabled -> " + dependency);
                    resourceProxies.put(dependency, previous);
                } else if (currentProvision.overrides().allowMappingOverride()) {
                    myProxies.add(resourceProxy);
                    overriddenDependencies.add(dependency);
                } else if (previousProvision.overrides().allowMappingOverride()) {
                    resourceProxies.put(dependency, previous);
                    overriddenDependencies.add(dependency);
                }
            } else {
                myProxies.add(resourceProxy);
            }
        }
    }

    <T> ResourceProxy<? extends T> get(Dependency<T> dependency) {
        return Generics.as(resourceProxies.get(dependency));
    }

    private <T> void putCollectionElement(final Dependency<T> dependency,
                                  ResourceProxy<? extends T> resourceProxy) {
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
        CollectionResourceProxy<T> collectionResourceProxy
                = Generics.as(resourceProxies.get(collectionDependency));
        if (collectionResourceProxy == null) {
            collectionResourceProxy = new CollectionResourceProxy<>(collectionDependency, dependency, ArrayList::new);
            resourceProxies.put(collectionDependency, collectionResourceProxy);
            myProxies.add(collectionResourceProxy);
        }
        collectionResourceProxy.add(resourceProxy);
    }

    @Override public GraphCache replicateWith(GraphContext context) {
        GraphCache newCache = new GraphCache(parentCache);
        for (ResourceProxy<?> proxy : myProxies) {
            newCache.putAll(proxy.replicateWith(context), context.errors());
        }
        return newCache;
    }

}
