package io.gunmetal.internal;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface Injectors {

    interface Factory {
        StaticInjector staticInjector(Method method, ComponentMetadata componentMetadata);
        <T> Injector<T> composite(ComponentMetadata componentMetadata);
        <T> Injector<T> lazy(ComponentMetadata componentMetadata);
        <T> Instantiator<T> constructorInstantiator(ComponentMetadata<Class> componentMetadata);
        <T> Instantiator<T> methodInstantiator(ComponentMetadata<Method> componentMetadata);
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
