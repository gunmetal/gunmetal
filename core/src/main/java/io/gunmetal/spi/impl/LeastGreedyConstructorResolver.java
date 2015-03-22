package io.gunmetal.spi.impl;

import io.gunmetal.spi.ConstructorResolver;

import java.lang.reflect.Constructor;

/**
 * @author rees.byars
 */
public final class LeastGreedyConstructorResolver implements ConstructorResolver {

    public LeastGreedyConstructorResolver() {
    }

    @Override public Constructor<?> resolve(Class<?> cls) {
        for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
            if (constructor.getParameterTypes().length == 0) {
                return constructor;
            }
        }
        return cls.getDeclaredConstructors()[0];
    }

}
