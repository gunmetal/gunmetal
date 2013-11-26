package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface InternalProvider {

    <T> ProvisionStrategy<T> getProvisionStrategy(DependencyRequest dependencyRequest);

}
