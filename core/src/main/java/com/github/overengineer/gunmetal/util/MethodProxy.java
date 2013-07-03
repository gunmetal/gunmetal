package com.github.overengineer.gunmetal.util;

import com.github.overengineer.gunmetal.inject.InjectionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public interface MethodProxy extends ParameterizedFunction {

    Object invoke(Object target, Object[] args);
    String getMethodName();
    Class<?> getDeclaringClass();

    class Factory {

        private static class Proxy implements MethodProxy {

            final MethodRef methodRef;

            Proxy(MethodRef methodRef) {
                this.methodRef = methodRef;
            }

            @Override
            public Object invoke(Object target, Object[] args) {
                try {
                    return methodRef.getMethod().invoke(target, args);
                } catch (Exception e) {
                    throw new InjectionException("Could not inject method [" + methodRef.getMethod().getName() + "] on component of type [" + target.getClass().getName() + "].", e);
                }
            }

            @Override
            public String getMethodName() {
                return methodRef.getMethod().getName();
            }

            @Override
            public Class<?> getDeclaringClass() {
                return methodRef.getMethod().getDeclaringClass();
            }

            @Override
            public Type[] getParameterTypes() {
                return methodRef.getMethod().getGenericParameterTypes();
            }

            @Override
            public Annotation[][] getParameterAnnotations() {
                return methodRef.getMethod().getParameterAnnotations();
            }
        }

        public static MethodProxy create(Method method) {
            return new Proxy(new MethodRefImpl(method));
        }

    }

}
