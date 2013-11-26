package io.gunmetal.internal;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;

/**
 * @author rees.byars
 */
abstract class ComponentMetadata<P extends AnnotatedElement> {

    abstract P provider();

    abstract Class<?> providerClass();

    abstract ModuleAdapter moduleAdapter();

    abstract Qualifier qualifier();

    abstract Collection<TypeKey<?>> targets();

    public int hashCode() {
        return provider().hashCode() * 67 + qualifier().hashCode();
    }

    public boolean equals(Object target) {
        if (target == this) {
            return true;
        }
        if (!(target instanceof ComponentMetadata<?>)) {
            return false;
        }
        ComponentMetadata<?> componentMetadataTarget = (ComponentMetadata<?>) target;
        return componentMetadataTarget.qualifier().equals(qualifier())
                && componentMetadataTarget.provider().equals(provider());
    }

}
