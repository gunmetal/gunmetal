package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ResourceMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rees.byars
 */
class ComponentGraph implements Replicable<ComponentGraph> {

    private final ResourceAccessorFactory resourceAccessorFactory;
    private final Map<Dependency, ResourceAccessor> resourceAccessors =
            new ConcurrentHashMap<>(64, .75f, 2);
    private final Set<Dependency> overriddenDependencies =
            Collections.newSetFromMap(new ConcurrentHashMap<>(0));

    ComponentGraph(ResourceAccessorFactory resourceAccessorFactory) {
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

        ResourceMetadata<?> newMetadata = resourceAccessor.binding().resource().metadata();

        if (newMetadata.isCollectionElement()) {
            putCollectionElement(dependency, resourceAccessor);
            return;
        }

        ResourceAccessor previous = resourceAccessors.put(dependency, resourceAccessor);
        if (previous == null) {
            return;
        }

        ResourceMetadata<?> prevMetadata = previous.binding().resource().metadata();

        // TODO better messages
        if (prevMetadata.overrides().allowMappingOverride() && newMetadata.overrides().allowMappingOverride()) {
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

    private void putCollectionElement(Dependency dependency, ResourceAccessor resourceAccessor) {
        Dependency collectionDependency =
                Dependency.from(dependency.qualifier(), dependency.typeKey().type(), List.class);
        CollectionResourceAccessor collectionResourceAccessor
                = (CollectionResourceAccessor) resourceAccessors.get(collectionDependency);
        if (collectionResourceAccessor == null) {
            collectionResourceAccessor = resourceAccessorFactory.createForCollection(collectionDependency, dependency);
            resourceAccessors.put(collectionDependency, collectionResourceAccessor);
        }
        collectionResourceAccessor.add(resourceAccessor);
    }

    @Override public ComponentGraph replicateWith(ComponentContext context) {
        ComponentGraph newRepo = new ComponentGraph(resourceAccessorFactory);
        for (ResourceAccessor resourceAccessor : resourceAccessors.values()) {
            newRepo.putAll(resourceAccessor.replicateWith(context), context.errors());
        }
        newRepo.overriddenDependencies.addAll(overriddenDependencies);
        return newRepo;
    }

}
