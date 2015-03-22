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

package io.gunmetal.spi;

import io.gunmetal.MultiBind;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
public interface Qualifier {

    Object[] qualifiers();

    Qualifier merge(Qualifier other);

    boolean intersects(Object[] qualifiers);

    boolean intersects(Qualifier qualifier);

    // TODO revisit the intersects impl here
    Qualifier NONE = new Qualifier() {

        Object[] qualifiers = {};

        @Override public Object[] qualifiers() {
            return qualifiers;
        }

        @Override public Qualifier merge(Qualifier other) {
            return other;
        }

        @Override public boolean intersects(Object[] qualifiers) {
            return true;
        }

        @Override public boolean intersects(Qualifier qualifier) {
            return true;
        }

        @Override public String toString() {
            return "qualifier[ NONE ]";
        }

    };

    static Qualifier from(AnnotatedElement annotatedElement,
                          Class<? extends Annotation> qualifierAnnotation) {
        List<Object> qualifiers = new LinkedList<>();
        for (Annotation annotation : annotatedElement.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(qualifierAnnotation) && !qualifiers.contains(annotation)) {
                qualifiers.add(annotation);
            } else if (annotationType == MultiBind.class) {
                qualifiers.add(annotation);
            }
        }
        if (qualifiers.isEmpty()) {
            return Qualifier.NONE;
        }
        return from(qualifiers.toArray());
    }

    static Qualifier from(final Object[] q) {
        Arrays.sort(q, (o1, o2) -> o1.getClass().getName().compareTo(o2.getClass().getName()));
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
                List<Object> qualifierList = new LinkedList<>();
                Collections.addAll(qualifierList, other.qualifiers());
                for (Object qualifier : qualifiers) {
                    if (!qualifierList.contains(qualifier)) {
                        qualifierList.add(qualifier);
                    }
                }
                return from(qualifierList.toArray());
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
                        && (this == o || Arrays.equals(((Qualifier) o).qualifiers(), qualifiers));
            }

            @Override public int hashCode() {
                return hashCode;
            }

            @Override public String toString() {
                return "qualifier[ " + Arrays.toString(qualifiers()) + " ]";
            }

        };

    }

}
