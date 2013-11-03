package io.gunmetal.builder;

import java.util.Arrays;

/**
 * @author rees.byars
 */
interface CompositeQualifier {

    Object[] getQualifiers();

    boolean intersects(Object[] qualifiers);

    class Factory {
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
                public boolean equals(Object o) {
                    return o instanceof CompositeQualifier && Arrays.equals(((CompositeQualifier) o).getQualifiers(), qualifiers);
                }
                @Override
                public int hashCode() {
                    return hashCode;
                }
            };
        }
    }

}
