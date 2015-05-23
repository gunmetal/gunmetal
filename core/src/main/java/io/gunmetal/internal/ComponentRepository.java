package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ResourceMetadata;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rees.byars
 */
class ComponentRepository implements Replicable<ComponentRepository> {

    private final ResourceAccessorFactory resourceAccessorFactory;
    private final Map<Dependency, ResourceAccessor> resourceAccessors = new ConcurrentHashMap<>(64, .75f, 2);
    private final Set<Dependency> overriddenDependencies = Collections.newSetFromMap(new ConcurrentHashMap<>(0));

    ComponentRepository(ResourceAccessorFactory resourceAccessorFactory) {
        this.resourceAccessorFactory = resourceAccessorFactory;
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
            return;
        }

        ResourceAccessor previous = resourceAccessors.put(dependency, resourceAccessor);
        if (previous == null) {
            return;
        }

        ResourceMetadata<?> prevMetadata =
                previous.binding().resource().metadata();

        // TODO better messages
        if (prevMetadata.overrides().allowMappingOverride()
                && newMetadata.overrides().allowMappingOverride()) {
            resourceAccessors.put(dependency, previous);
            errors.add("more than one of type with override enabled -> " + dependency);
        } else if (
                (overriddenDependencies.contains(dependency)
                        && !newMetadata.overrides().allowMappingOverride())
                        || (!prevMetadata.overrides().allowMappingOverride()
                        && !newMetadata.overrides().allowMappingOverride())) {
            resourceAccessors.put(dependency, previous);
            errors.add("more than one of type without override enabled -> " + dependency);
        } else if (newMetadata.overrides().allowMappingOverride()) {
            overriddenDependencies.add(dependency);
        } else if (prevMetadata.overrides().allowMappingOverride()) {
            resourceAccessors.put(dependency, previous);
            overriddenDependencies.add(dependency);
        }
    }

    ResourceAccessor get(Dependency dependency) {
        return resourceAccessors.get(dependency);
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
                = (CollectionResourceAccessor) resourceAccessors.get(collectionDependency);
        if (collectionDependencyService == null) {
            collectionDependencyService = resourceAccessorFactory.createForCollection(collectionDependency, dependency);
            resourceAccessors.put(collectionDependency, collectionDependencyService);
        }
        collectionDependencyService.add(resourceAccessor);
    }

    @Override public ComponentRepository replicateWith(ComponentContext context) {
        ComponentRepository newRepo =
                new ComponentRepository(resourceAccessorFactory);
        for (ResourceAccessor resourceAccessor : resourceAccessors.values()) {
            newRepo.putAll(resourceAccessor.replicateWith(context), context.errors());
        }
        newRepo.overriddenDependencies.addAll(overriddenDependencies);
        return newRepo;
    }

}
