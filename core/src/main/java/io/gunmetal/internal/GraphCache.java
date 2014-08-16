package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ProvisionMetadata;
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
    private final Map<Dependency<?>, DependencyRequestHandler<?>> requestHandlers = new ConcurrentHashMap<>(64, .75f, 2);
    private final Set<Dependency<?>> overriddenDependencies = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Queue<DependencyRequestHandler<?>> myHandlers = new LinkedList<>();

    GraphCache(GraphCache parentCache) {
        this.parentCache = parentCache;
        if (parentCache != null) {
            requestHandlers.putAll(parentCache.requestHandlers);
        }
    }

    void putAll(List<DependencyRequestHandler<?>> requestHandlers, Errors errors) {
        for (DependencyRequestHandler<?> requestHandler : requestHandlers) {
            putAll(requestHandler, errors);
        }
    }

    <T> void putAll(DependencyRequestHandler<T> requestHandler, Errors errors) {
        for (Dependency<? super T> dependency : requestHandler.targets()) {
            put(dependency, requestHandler, errors);
        }
    }

    <T> void put(final Dependency<? super T> dependency, DependencyRequestHandler<T> requestHandler, Errors errors) {
        ProvisionMetadata<?> currentProvision = requestHandler.provisionMetadata();
        if (currentProvision.isCollectionElement()) {
            putCollectionElement(dependency, requestHandler);
        } else {
            DependencyRequestHandler<?> previous = requestHandlers.put(dependency, requestHandler);
            if (previous != null) {
                ProvisionMetadata<?> previousProvision = previous.provisionMetadata();
                // TODO better messages, include provisions, keep list?
                if (previousProvision.overrides().allowMappingOverride()
                        && currentProvision.overrides().allowMappingOverride()) {
                    errors.add("more than one of type with override enabled");
                    requestHandlers.put(dependency, previous);
                } else if (
                        (overriddenDependencies.contains(dependency)
                                && !currentProvision.overrides().allowMappingOverride())
                        || (!previousProvision.overrides().allowMappingOverride()
                                && !currentProvision.overrides().allowMappingOverride())) {
                    errors.add("more than one of type without override enabled");
                    requestHandlers.put(dependency, previous);
                } else if (currentProvision.overrides().allowMappingOverride()) {
                    myHandlers.add(requestHandler);
                    overriddenDependencies.add(dependency);
                } else if (previousProvision.overrides().allowMappingOverride()) {
                    requestHandlers.put(dependency, previous);
                    overriddenDependencies.add(dependency);
                }
            } else {
                myHandlers.add(requestHandler);
            }
        }
    }

    <T> DependencyRequestHandler<? extends T> get(Dependency<T> dependency) {
        return Generics.as(requestHandlers.get(dependency));
    }

    private <T> void putCollectionElement(final Dependency<T> dependency,
                                  DependencyRequestHandler<? extends T> requestHandler) {
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
        CollectionRequestHandler<T> collectionRequestHandler
                = Generics.as(requestHandlers.get(collectionDependency));
        if (collectionRequestHandler == null) {
            collectionRequestHandler = new CollectionRequestHandler<>(collectionDependency, dependency, ArrayList::new);
            requestHandlers.put(collectionDependency, collectionRequestHandler);
            myHandlers.add(collectionRequestHandler);
        }
        collectionRequestHandler.add(requestHandler);
    }

    @Override public GraphCache replicateWith(GraphContext context) {
        GraphCache newCache = new GraphCache(parentCache);
        for (DependencyRequestHandler<?> handler : myHandlers) {
            newCache.putAll(handler.replicateWith(context), context.errors());
        }
        return newCache;
    }

}
