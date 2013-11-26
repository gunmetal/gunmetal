package io.gunmetal.internal;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface Injectors {

    interface Factory {
        StaticInjector staticInjector(Method method,
                                      ComponentMetadata componentMetadata,
                                      InternalProvider internalProvider);
        <T> Injector<T> composite(ComponentMetadata componentMetadata, InternalProvider internalProvider);
        <T> Injector<T> lazy(ComponentMetadata componentMetadata);
        <T> Instantiator<T> instantiator(Class cls,
                                         ComponentMetadata componentMetadata,
                                         InternalProvider internalProvider);
        <T> Instantiator<T> instantiator(Method method,
                                         ComponentMetadata componentMetadata,
                                         InternalProvider internalProvider);
    }

    interface StaticInjector {
        Object inject(InternalProvider internalProvider, ResolutionContext resolutionContext);
    }

    interface Injector<T> {
        Object inject(T target, InternalProvider internalProvider, ResolutionContext resolutionContext);
    }

    interface Instantiator<T> {

        T getInstance(InternalProvider provider, ResolutionContext resolutionContext);

        T getInstance(InternalProvider provider, ResolutionContext resolutionContext, Object... providedArgs);

    }

}
