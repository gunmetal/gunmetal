/*
 * Copyright (c) 2013.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gunmetal.internal;

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
final class Smithy {

    private Smithy() { }

    static Qualifier qualifier(AnnotatedElement annotatedElement,
                                     Class<? extends Annotation> qualifierAnnotation) {
        List<Object> qualifiers = new LinkedList<Object>();
        for (Annotation annotation : annotatedElement.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(qualifierAnnotation) && !qualifiers.contains(annotation)) {
                qualifiers.add(annotation);
            }
        }
        return qualifier(qualifiers.toArray());
    }

    static Qualifier qualifier(AnnotatedElement annotatedElement, Qualifier joiningQualifier,
                                     Class<? extends Annotation> qualifierAnnotation) {
        List<Object> qualifiers = new LinkedList<Object>();
        Collections.addAll(qualifiers, joiningQualifier.qualifiers());
        for (Annotation annotation : annotatedElement.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(qualifierAnnotation) && !qualifiers.contains(annotation)) {
                qualifiers.add(annotation);
            }
        }
        return qualifier(qualifiers.toArray());
    }

    static Qualifier qualifier(final Object[] q) {
        Arrays.sort(q);
        return new Qualifier() {

            Object[] qualifiers = q;
            int hashCode = Arrays.hashCode(qualifiers);

            @Override public Object[] qualifiers() {
                return qualifiers;
            }

            @Override public Qualifier merge(Qualifier other) {
                if (qualifiers.length == 0) {
                    return other;
                }
                if (other.qualifiers().length == 0) {
                    return this;
                }
                List<Object> qualifierList = new LinkedList<Object>();
                Collections.addAll(qualifierList, other.qualifiers());
                for (Object qualifier : qualifiers) {
                    if (!qualifierList.contains(qualifier)) {
                        qualifierList.add(qualifier);
                    }
                }
                return qualifier(qualifierList.toArray());
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

            @Override public boolean intersects(Qualifier qualifier) {
                return intersects(qualifier.qualifiers());
            }

            @Override public boolean equals(Object o) {
                return o instanceof Qualifier
                        && Arrays.equals(((Qualifier) o).qualifiers(), qualifiers);
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

    static <T> Dependency<T> dependency(Class<? super T> raw, Qualifier qualifier) {
        return null;
    }

    static <T> Dependency<T> dependency(Type type, Qualifier qualifier) {
        return null;
    }

}
