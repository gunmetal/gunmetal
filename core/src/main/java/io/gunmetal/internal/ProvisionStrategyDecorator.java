package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface ProvisionStrategyDecorator {

    <T> ProvisionStrategy<T> decorate(ComponentMetadata componentMetadata,
                                      ProvisionStrategy<T> delegateStrategy,
                                      InternalProvider internalProvider);

}
