package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface ComponentAdapterFactory {

    <T> ComponentAdapter<T> create(ComponentMetadata componentMetadata,
                                   AccessFilter<DependencyRequest> accessFilter,
                                   InternalProvider internalProvider);

}
