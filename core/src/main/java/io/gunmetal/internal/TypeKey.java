package io.gunmetal.internal;

import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
interface TypeKey<T> {
    Type type();
    Class<? super T> raw();
    // TODO boolean isAssignableFrom(TypeKey<?> typeKey);
}
