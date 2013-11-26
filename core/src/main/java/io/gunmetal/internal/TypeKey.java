package io.gunmetal.internal;

import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
abstract class TypeKey<T> {

    abstract Type type();

    abstract Class<? super T> raw();

    @Override public int hashCode() {
        return type().hashCode();
    }

    @Override public boolean equals(Object target) {
        if (target == this) {
            return true;
        }
        if (!(target instanceof TypeKey<?>)) {
            return false;
        }
        TypeKey<?> typeKeyTarget = (TypeKey<?>) target;
        return type().equals(typeKeyTarget.type());
    }

}
