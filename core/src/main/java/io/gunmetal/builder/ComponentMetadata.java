package io.gunmetal.builder;

import com.github.overengineer.gunmetal.scope.Scope;

/**
 * @author rees.byars
 */
interface ComponentMetadata<T> {

    Class<T> getComponentClass();

    ModuleAdapter getModuleAdapter();

    Scope getScope();

    CompositeQualifier getCompositeQualifier();

}
