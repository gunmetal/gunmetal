package com.github.overengineer.gunmetal.util;

import com.github.overengineer.gunmetal.inject.InjectionException;
import com.github.overengineer.gunmetal.key.Dependency;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
public interface FieldProxy extends Serializable, TypeRef {

    void set(Object target, Object value);
    Object get(Object target);
    Class<?> getDeclaringClass();
    boolean isDecorated();

    class Factory {

        private static class Proxy implements FieldProxy {

            private static final long serialVersionUID = -699981356150321799L;
            final FieldRef fieldRef;

            Proxy(FieldRef fieldRef) {
                this.fieldRef = fieldRef;
            }

            @Override
            public void set(Object target, Object value) {
                try {
                    fieldRef.getField().set(target, value);
                }  catch (Exception e) {
                    throw new InjectionException("Could not inject field [" + fieldRef.getField().getName() + "] on component of type [" + target.getClass().getName() + "].", e);
                }
            }

            @Override
            public Object get(Object target) {
                try {
                    return fieldRef.getField().get(target);
                }  catch (Exception e) {
                    throw new InjectionException("Could not retrieve field [" + fieldRef.getField().getName() + "] from component of type [" + target.getClass().getName() + "].", e);
                }
            }

            @Override
            public Type getType() {
                return fieldRef.getField().getGenericType();
            }

            @Override
            public Class<?> getDeclaringClass() {
                return fieldRef.getField().getDeclaringClass();
            }

            @Override
            public boolean isDecorated() {
                return fieldRef.getField().getType().isAssignableFrom(fieldRef.getField().getDeclaringClass());
            }
        }

        public static FieldProxy create(Field field) {
            return new Proxy(new FieldRefImpl(field));
        }

        public static FieldProxy create(MethodProxy methodProxy) {
            Type[] paramTypes = methodProxy.getParameterTypes();
            if (paramTypes.length != 1) {
                return null;
            }
            Class<?> paramType = ReflectionUtil.getRawClass(paramTypes[0]);
            List<Field> matchingFields = new LinkedList<Field>();
            for (Field field : methodProxy.getDeclaringClass().getDeclaredFields()) {
                if (field.getType().isAssignableFrom(paramType)) {
                    matchingFields.add(field);
                }
            }
            if (matchingFields.size() != 1) {
                return null;
            }
            return new Proxy(new FieldRefImpl(matchingFields.get(0)));
        }

        public static FieldProxy create(Dependency dependency, Object target) {
            Class<?> paramType = dependency.getTypeKey().getRaw();
            List<Field> matchingFields = new LinkedList<Field>();
            for (Field field : target.getClass().getDeclaredFields()) {
                if (field.getType().isAssignableFrom(paramType)) {
                    matchingFields.add(field);
                }
            }
            if (matchingFields.size() != 1) {
                return null;
            }
            return new Proxy(new FieldRefImpl(matchingFields.get(0)));
        }

    }

}
