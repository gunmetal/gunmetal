package io.gunmetal.spi.impl;

import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.spi.InjectionResolver;
import io.gunmetal.util.Generics;

import java.lang.reflect.Constructor;

/**
 * @author rees.byars
 */
public final class ExactlyOneConstructorResolver implements ConstructorResolver {

    private final InjectionResolver injectionResolver;

    public ExactlyOneConstructorResolver(InjectionResolver injectionResolver) {
        this.injectionResolver = injectionResolver;
    }

    @Override public <T> Constructor<T> resolve(Class<T> cls) {
        Constructor<?>[] constructors = cls.getDeclaredConstructors();
        if (constructors.length == 1 && constructors[0].getParameterCount() == 0) {
            return Generics.as(constructors[0]);
        }
        Constructor<T> theOne = null;
        for (Constructor<?> constructor : constructors) {
            if (injectionResolver.shouldInject(constructor)) {
                if (theOne != null) {
                    throw new IllegalArgumentException("More than one constructor marked for injection [" + cls + "]");
                } else {
                    theOne = Generics.as(constructor);
                }
            }
        }
        return theOne;
    }

}
