package io.gunmetal.internal;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface Injector {

    interface Factory {
        StaticMethod create(Method method, ComponentMetadata componentMetadata, InternalProvider internalProvider);
    }

    interface StaticMethod {
        Object inject(InternalProvider internalProvider, ResolutionContext resolutionContext);
    }

}
