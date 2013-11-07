package io.gunmetal;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
public interface CompositeQualifier {

    Object[] getQualifiers();

    boolean intersects(Object[] qualifiers);

    boolean intersects(CompositeQualifier compositeQualifier);

    final class Factory {

        private Factory() { }

        public static CompositeQualifier create(Class<?> cls, Class<? extends Annotation> qualifierAnnotation) {
            List<Object> qualifiers = new LinkedList<Object>();
            for (Annotation annotation : cls.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (annotationType.isAnnotationPresent(qualifierAnnotation)) {
                    qualifiers.add(annotation);
                }
            }
            return create(qualifiers.toArray());
        }

        public static CompositeQualifier create(final Object[] qualifiers) {

            Arrays.sort(qualifiers);

            return new CompositeQualifier() {

                int hashCode = Arrays.hashCode(qualifiers);

                @Override
                public Object[] getQualifiers() {
                    return qualifiers;
                }

                @Override
                public boolean intersects(Object[] otherQualifiers) {
                    for (Object qualifier : qualifiers) {
                        for (Object otherQualifier : otherQualifiers) {
                            if (qualifier.equals(otherQualifier)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }

                @Override
                public boolean intersects(CompositeQualifier compositeQualifier) {
                    return intersects(compositeQualifier.getQualifiers());
                }

                @Override
                public boolean equals(Object o) {
                    return o instanceof CompositeQualifier
                            && Arrays.equals(((CompositeQualifier) o).getQualifiers(), qualifiers);
                }

                @Override
                public int hashCode() {
                    return hashCode;
                }

            };
        }
    }

}
