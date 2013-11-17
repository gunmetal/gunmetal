package io.gunmetal.internal;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface Injectors {

    interface Factory {
        StaticInjector staticInjector(Method method, ComponentMetadata componentMetadata, InternalProvider internalProvider);
    }

    interface StaticInjector {
        Object inject(InternalProvider internalProvider, ResolutionContext resolutionContext);
    }

    interface Injector<T> {
        Object inject(T target, InternalProvider internalProvider, ResolutionContext resolutionContext);
    }

}
