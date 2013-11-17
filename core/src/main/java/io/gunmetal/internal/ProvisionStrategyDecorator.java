package io.gunmetal.internal;

import java.lang.reflect.AnnotatedElement;

/**
 * @author rees.byars
 */
interface ProvisionStrategyDecorator {

    <T> ProvisionStrategy<T> decorate(AnnotatedElement annotatedElement,
                                      ComponentMetadata componentMetadata,
                                      ProvisionStrategy<T> delegateStrategy);

}
