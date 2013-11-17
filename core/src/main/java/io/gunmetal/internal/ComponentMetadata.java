package io.gunmetal.internal;

import io.gunmetal.CompositeQualifier;

/**
 * @author rees.byars
 */
interface ComponentMetadata {

    Object origin();

    Class<?> originClass();

    ModuleAdapter moduleAdapter();

    CompositeQualifier compositeQualifier();

    int hashCode();

    boolean equals(Object target);

}
