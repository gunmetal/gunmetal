package io.gunmetal.internal;

import io.gunmetal.CompositeQualifier;

import java.util.Collection;

/**
 * @author rees.byars
 */
abstract class ComponentMetadata<P> {

    abstract P provider();

    abstract Class<?> providerClass();

    abstract ModuleAdapter moduleAdapter();

    abstract CompositeQualifier qualifier();

    abstract Collection<TypeKey<?>> targets();

    public int hashCode() {
        return provider().hashCode() * 67 + qualifier().hashCode();
    }

    public boolean equals(Object target) {
        return target instanceof ComponentMetadata
                && ((ComponentMetadata) target).qualifier().equals(qualifier())
                && ((ComponentMetadata) target).provider().equals(provider());
    }

}
