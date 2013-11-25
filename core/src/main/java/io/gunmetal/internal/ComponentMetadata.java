package io.gunmetal.internal;

import io.gunmetal.CompositeQualifier;

import java.lang.reflect.Type;
import java.util.Collection;

/**
 * @author rees.byars
 */
interface ComponentMetadata<P> {

    P provider();

    Class<?> providerClass();

    ModuleAdapter moduleAdapter();

    CompositeQualifier qualifier();

    Collection<Type> targets();

    int hashCode();

    boolean equals(Object target);

}
