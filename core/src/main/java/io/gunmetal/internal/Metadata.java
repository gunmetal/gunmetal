package io.gunmetal.internal;

import io.gunmetal.CompositeQualifier;
import io.gunmetal.Dependency;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
final class Metadata {

    private Metadata() { }

    static CompositeQualifier qualifier(AnnotatedElement annotatedElement,
                                     Class<? extends Annotation> qualifierAnnotation) {
        List<Object> qualifiers = new LinkedList<Object>();
        for (Annotation annotation : annotatedElement.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(qualifierAnnotation)) {
                qualifiers.add(annotation);
            }
        }
        return qualifier(qualifiers.toArray());
    }

    static CompositeQualifier qualifier(AnnotatedElement annotatedElement, ModuleAdapter moduleAdapter,
                                     Class<? extends Annotation> qualifierAnnotation) {
        List<Object> qualifiers = new LinkedList<Object>();
        Collections.addAll(qualifiers, moduleAdapter.getCompositeQualifier().getQualifiers());
        for (Annotation annotation : annotatedElement.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(qualifierAnnotation)) {
                qualifiers.add(annotation);
            }
        }
        return qualifier(qualifiers.toArray());
    }

    static CompositeQualifier qualifier(final Object[] qualifiers) {

        Arrays.sort(qualifiers);

        return new CompositeQualifier() {

            int hashCode = Arrays.hashCode(qualifiers);

            @Override public Object[] getQualifiers() {
                return qualifiers;
            }

            @Override public boolean intersects(Object[] otherQualifiers) {
                for (Object qualifier : qualifiers) {
                    for (Object otherQualifier : otherQualifiers) {
                        if (qualifier.equals(otherQualifier)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override public boolean intersects(CompositeQualifier compositeQualifier) {
                return intersects(compositeQualifier.getQualifiers());
            }

            @Override public boolean equals(Object o) {
                return o instanceof CompositeQualifier
                        && Arrays.equals(((CompositeQualifier) o).getQualifiers(), qualifiers);
            }

            @Override public int hashCode() {
                return hashCode;
            }

        };

    }

    static Annotation scope(AnnotatedElement annotatedElement, Class<? extends Annotation> scopeAnnotation) {
        for (Annotation annotation : annotatedElement.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(scopeAnnotation)) {
                return annotation;
            }
        }
        return null;
    }

    static <T> Dependency<T> dependency(Class<? super T> raw, CompositeQualifier compositeQualifier) {
        return null;
    }

    static <T> Dependency<T> dependency(Type type, CompositeQualifier compositeQualifier) {
        return null;
    }

}
