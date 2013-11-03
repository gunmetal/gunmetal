package io.gunmetal.builder;

import com.github.overengineer.gunmetal.scope.Scope;

/**
 * @author rees.byars
 */
interface ComponentAdapter<T> extends AccessFilter<ComponentAdapter>, ProvisionStrategy<T> {

    // TODO instead of making visibility adapter accessible, add Dependency to ResolutionContext,
    // TODO and add requesting ComponentAdapter to Dependency?

    Class<T> getComponentClass();
    ModuleAdapter getModuleAdapter();
    Scope getScope();
    CompositeQualifier getCompositeQualifier();
}
