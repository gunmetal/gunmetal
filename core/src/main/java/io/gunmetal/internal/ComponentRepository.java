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
class ComponentRepository implements Replicable<ComponentRepository> {

    private final ResourceAccessorFactory resourceAccessorFactory;
    private final ComponentRepository parentRepository;     
    private final Map<Dependency, ResourceAccessor> dependencyServices = new ConcurrentHashMap<>(64, .75f, 2);
    private final Set<Dependency> overriddenDependencies = Collections.newSetFromMap(new ConcurrentHashMap<>(0));
    private final Queue<ResourceAccessor> myResourceAccessors = new LinkedList<>();

    ComponentRepository(ResourceAccessorFactory resourceAccessorFactory,
                        ComponentRepository parentRepository) {
        this.resourceAccessorFactory = resourceAccessorFactory;
        this.parentRepository = parentRepository;
        if (parentRepository != null && dependencyServices.isEmpty()) {
            dependencyServices.putAll(parentRepository.dependencyServices);
        }
    }

    void putAll(List<ResourceAccessor> resourceAccessors, Errors errors) {
        for (ResourceAccessor resourceAccessor : resourceAccessors) {
            putAll(resourceAccessor, errors);
        }
    }

    void putAll(ResourceAccessor resourceAccessor, Errors errors) {
        for (Dependency dependency : resourceAccessor.binding().targets()) {
            put(dependency, resourceAccessor, errors);
        }
    }

    void put(final Dependency dependency, ResourceAccessor resourceAccessor, Errors errors) {
        ResourceMetadata<?> newMetadata = 
                resourceAccessor.binding().resource().metadata();
        if (newMetadata.isCollectionElement()) {
            putCollectionElement(dependency, resourceAccessor);
        } else {
            ResourceAccessor previous = dependencyServices.put(dependency, resourceAccessor);
            if (previous != null) {

                ResourceMetadata<?> prevMetadata =
                        previous.binding().resource().metadata();
                
                // TODO better messages, include provisions, keep list?
                if (prevMetadata.overrides().allowMappingOverride()
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
                    myResourceAccessors.add(resourceAccessor);
                    overriddenDependencies.add(dependency);
                } else if (prevMetadata.overrides().allowMappingOverride()) {
                    dependencyServices.put(dependency, previous);
                    overriddenDependencies.add(dependency);
                }
            } else {
                myResourceAccessors.add(resourceAccessor);
            }
        }
    }

    ResourceAccessor get(Dependency dependency) {
        return dependencyServices.get(dependency);
    }

    private void putCollectionElement(final Dependency dependency,
                                      ResourceAccessor resourceAccessor) {
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
        CollectionResourceAccessor collectionDependencyService
                = (CollectionResourceAccessor) dependencyServices.get(collectionDependency);
        if (collectionDependencyService == null) {
            collectionDependencyService = resourceAccessorFactory.createForCollection(collectionDependency, dependency);
            dependencyServices.put(collectionDependency, collectionDependencyService);
            myResourceAccessors.add(collectionDependencyService);
        }
        collectionDependencyService.add(resourceAccessor);
    }

    @Override public ComponentRepository replicateWith(ComponentContext context) {
        ComponentRepository newRepo =
                new ComponentRepository(resourceAccessorFactory, parentRepository);
        for (ResourceAccessor resourceAccessor : myResourceAccessors) {
            newRepo.putAll(resourceAccessor.replicateWith(context), context.errors());
        }
        return newRepo;
    }

}
