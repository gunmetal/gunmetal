package io.gunmetal.builder;

import java.util.Arrays;

/**
 * @author rees.byars
 */
interface QualifierAdapter {

    Object[] getQualifiers();

    class Factory {
        public static QualifierAdapter create(final Object[] qualifiers) {
            Arrays.sort(qualifiers);
            return new QualifierAdapter() {
                int hashCode = Arrays.hashCode(qualifiers);
                @Override
                public Object[] getQualifiers() {
                    return qualifiers;
                }
                @Override
                public boolean equals(Object o) {
                    return o instanceof QualifierAdapter && Arrays.equals(((QualifierAdapter) o).getQualifiers(), qualifiers);
                }
                @Override
                public int hashCode() {
                    return hashCode;
                }
            };
        }
    }

}
