package io.gunmetal.internal;

import java.lang.reflect.AnnotatedElement;

/**
 * @author rees.byars
 */
interface ProvisionStrategyFactory {

    <T, P extends AnnotatedElement> ProvisionStrategy<T> create(
            ComponentMetadata<P> componentMetadata,
            InternalProvider internalProvider);

}
