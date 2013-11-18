package io.gunmetal.internal;

import io.gunmetal.CompositeQualifier;

import java.lang.reflect.AnnotatedElement;

/**
 * @author rees.byars
 */
interface ComponentMetadata {

    AnnotatedElement provider();

    Class<?> providerClass();

    ModuleAdapter moduleAdapter();

    CompositeQualifier compositeQualifier();

    int hashCode();

    boolean equals(Object target);

}
