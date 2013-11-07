package io.gunmetal.internal;

import io.gunmetal.CompositeQualifier;
import io.gunmetal.Scope;

/**
 * @author rees.byars
 */
interface ComponentMetadata<T> {

    Class<T> getComponentClass();

    ModuleAdapter getModuleAdapter();

    Scope getScope();

    CompositeQualifier getCompositeQualifier();

}
