package io.gunmetal.builder;

/**
 * @author rees.byars
 */
interface ComponentAdapter<T> extends ComponentMetadata<T>, ProvisionStrategy<T>, AccessFilter<DependencyRequest> {

}
