package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ResourceMetadata;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
    private final Map<Dependency, DependencyService> dependencyServices = new ConcurrentHashMap<>(64, .75f, 2);
    private final Set<Dependency> overriddenDependencies = Collections.newSetFromMap(new ConcurrentHashMap<>(0));
    private final Queue<DependencyService> myDependencyServices = new LinkedList<>();
    private final DependencyServiceFactory dependencyServiceFactory;

    GraphCache(DependencyServiceFactory dependencyServiceFactory, 
               GraphCache parentCache) {
        this.dependencyServiceFactory = dependencyServiceFactory;
        this.parentCache = parentCache;
        if (parentCache != null) {
            dependencyServices.putAll(parentCache.dependencyServices);
        }
    }

    void putAll(List<DependencyService> dependencyServices, Errors errors) {
        for (DependencyService dependencyService : dependencyServices) {
            putAll(dependencyService, errors);
        }
    }

    void putAll(DependencyService dependencyService, Errors errors) {
        for (Dependency dependency : dependencyService.binding().targets()) {
            put(dependency, dependencyService, errors);
        }
    }

    void put(final Dependency dependency, DependencyService dependencyService, Errors errors) {
        ResourceMetadata<?> newMetadata = 
                dependencyService.binding().resource().metadata();
        if (newMetadata.isCollectionElement()) {
            putCollectionElement(dependency, dependencyService);
        } else {
            DependencyService previous = dependencyServices.put(dependency, dependencyService);
            if (previous != null) {

                ResourceMetadata<?> prevMetadata =
                        previous.binding().resource().metadata();
                
                // TODO better messages, include provisions, keep list?
                if (prevMetadata.isModule()) { // TODO this is a hack that depends on the order from the dependencyService factory
                    myDependencyServices.add(dependencyService);
                } else if (prevMetadata.overrides().allowMappingOverride()
                        && newMetadata.overrides().allowMappingOverride()) {
                    errors.add("more than one of type with override enabled -> " + dependency);
                    dependencyServices.put(dependency, previous);
                } else if (
                        (overriddenDependencies.contains(dependency)
                                && !newMetadata.overrides().allowMappingOverride())
                                || (!prevMetadata.overrides().allowMappingOverride()
                                && !newMetadata.overrides().allowMappingOverride())) {
                    errors.add("more than one of type without override enabled -> " + dependency);
                    dependencyServices.put(dependency, previous);
                } else if (newMetadata.overrides().allowMappingOverride()) {
                    myDependencyServices.add(dependencyService);
                    overriddenDependencies.add(dependency);
                } else if (prevMetadata.overrides().allowMappingOverride()) {
                    dependencyServices.put(dependency, previous);
                    overriddenDependencies.add(dependency);
                }
            } else {
                myDependencyServices.add(dependencyService);
            }
        }
    }

    DependencyService get(Dependency dependency) {
        return dependencyServices.get(dependency);
    }

    private void putCollectionElement(final Dependency dependency,
                                      DependencyService dependencyService) {
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
        CollectionDependencyService collectionDependencyService
                = (CollectionDependencyService) dependencyServices.get(collectionDependency);
        if (collectionDependencyService == null) {
            collectionDependencyService = dependencyServiceFactory.createForCollection(collectionDependency, dependency);
            dependencyServices.put(collectionDependency, collectionDependencyService);
            myDependencyServices.add(collectionDependencyService);
        }
        collectionDependencyService.add(dependencyService);
    }

    @Override public GraphCache replicateWith(GraphContext context) {
        GraphCache newCache = new GraphCache(dependencyServiceFactory, parentCache);
        for (DependencyService dependencyService : myDependencyServices) {
            newCache.putAll(dependencyService.replicateWith(context), context.errors());
        }
        return newCache;
    }

}
