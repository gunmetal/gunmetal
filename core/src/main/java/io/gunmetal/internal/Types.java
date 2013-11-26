package io.gunmetal.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author rees.byars
 */
final class Types {

    private Types() { }

    static <T> TypeKey<T> typeKey(final Class<T> cls) {
        return new TypeKey<T>() {
            @Override public Type type() {
                return cls;
            }
            @Override public Class<? super T> raw() {
                return cls;
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <T> TypeKey<T> typeKey(final Type type) {
        if (type instanceof Class) {
            return typeKey((Class) type);
        } else if (type instanceof ParameterizedType) {
            return typeKey(((ParameterizedType) type));
        } else {
            throw new UnsupportedOperationException("The type [" + type + "] is currently unsupported");
        }
    }

    @SuppressWarnings("unchecked")
    static <T> TypeKey<T> typeKey(final ParameterizedType type) {
        final Class<? super T> raw = (Class<? super T>) type.getRawType();
        return new TypeKey<T>() {
            @Override public Type type() {
                return type;
            }
            @Override public Class<? super T> raw() {
                return raw;
            }
        };
    }

    static Collection<TypeKey<?>> typeKeys(final Class<?>[] classes) {
        List<TypeKey<?>> typeKeys = new ArrayList<TypeKey<?>>();
        for (Class<?> cls : classes) {
            typeKeys.add(typeKey(cls));
        }
        return typeKeys;
    }

}
