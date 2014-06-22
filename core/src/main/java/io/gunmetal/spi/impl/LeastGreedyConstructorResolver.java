package io.gunmetal.spi.impl;

import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.util.Generics;

import java.lang.reflect.Constructor;

/**
 * @author rees.byars
 */
public final class LeastGreedyConstructorResolver implements ConstructorResolver {

    public LeastGreedyConstructorResolver() { }

    @Override public <T> Constructor<T> resolve(Class<T> cls) {
        for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
            if (constructor.getParameterTypes().length == 0) {
                return Generics.as(constructor);
            }
        }
        return Generics.as(cls.getDeclaredConstructors()[0]);
    }

}
