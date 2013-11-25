package io.gunmetal.internal;

import io.gunmetal.CompositeQualifier;

import java.util.Collection;

/**
 * @author rees.byars
 */
interface ComponentMetadata<P> {

    P provider();

    Class<?> providerClass();

    ModuleAdapter moduleAdapter();

    CompositeQualifier qualifier();

    Collection<TypeKey<?>> targets();

    int hashCode();

    boolean equals(Object target);

}
