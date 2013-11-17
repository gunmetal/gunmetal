package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface ComponentAdapter<T> extends ProvisionStrategy<T>, AccessFilter<DependencyRequest> {

    ComponentMetadata metadata();

}
