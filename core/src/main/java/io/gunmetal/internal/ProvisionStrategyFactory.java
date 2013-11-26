package io.gunmetal.internal;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface ProvisionStrategyFactory {

    <T> ProvisionStrategy<T> withClassProvider(ComponentMetadata<Class> componentMetadata);
    <T> ProvisionStrategy<T> withMethodProvider(ComponentMetadata<Method> componentMetadata);

}
