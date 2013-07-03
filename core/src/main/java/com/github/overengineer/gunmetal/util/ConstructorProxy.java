package com.github.overengineer.gunmetal.util;

import com.github.overengineer.gunmetal.inject.InjectionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public interface ConstructorProxy<T> extends ParameterizedFunction {

    T newInstance(Object[] args);

    class Factory {

        private static class Proxy<T> implements ConstructorProxy<T> {

            final ConstructorRef<T> constructorRef;

            Proxy(ConstructorRef<T> constructorRef) {
                this.constructorRef = constructorRef;
            }

            @Override
            public T newInstance(Object[] args) {
                try {
                    return constructorRef.getConstructor().newInstance(args);
                } catch (Exception e) {
                    throw new InjectionException("Could not create new instance of type [" + constructorRef.getConstructor().getDeclaringClass().getName() + "]", e);
                }
            }

            @Override
            public Type[] getParameterTypes() {
                return constructorRef.getConstructor().getGenericParameterTypes();
            }

            @Override
            public Annotation[][] getParameterAnnotations() {
                return constructorRef.getConstructor().getParameterAnnotations();
            }
        }

        public static <T> ConstructorProxy<T> create(Constructor<T> constructor) {
            return new Proxy<T>(new ConstructorRefImpl<T>(constructor));
        }

    }
}
