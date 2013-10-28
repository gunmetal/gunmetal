package com.github.overengineer.gunmetal.adapter;

import java.util.Arrays;

/**
 * @author rees.byars
 */
public interface QualifierAdapter {

    Object[] getQualifiers();

    class Factory {
        public static QualifierAdapter create(final Object[] qualifiers) {
            Arrays.sort(qualifiers);
            return new QualifierAdapter() {
                @Override
                public Object[] getQualifiers() {
                    return qualifiers;
                }
                @Override
                public boolean equals(Object o) {
                    return o instanceof QualifierAdapter && Arrays.equals(((QualifierAdapter) o).getQualifiers(), qualifiers);
                }
            };
        }
    }

}
