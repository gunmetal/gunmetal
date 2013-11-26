package io.gunmetal.internal;

import java.lang.reflect.AnnotatedElement;

/**
 * @author rees.byars
 */
interface ProvisionStrategyDecorator {

    <T,P extends AnnotatedElement> ProvisionStrategy<T> decorate(
            ComponentMetadata<P> componentMetadata,
            ProvisionStrategy<T> delegateStrategy);

}
