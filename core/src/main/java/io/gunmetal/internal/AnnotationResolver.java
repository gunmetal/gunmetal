package io.gunmetal.internal;

import java.lang.reflect.AnnotatedElement;

/**
 * @author rees.byars
 */
interface AnnotationResolver<T> {
    T resolve(AnnotatedElement annotatedElement);
    T resolve(AnnotatedElement annotatedElement, T t);
}
