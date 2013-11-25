package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface ComponentAdapter<T> {

    ComponentMetadata metadata();

    AccessFilter<DependencyRequest> filter();

    ProvisionStrategy<T> provisionStrategy();

}
