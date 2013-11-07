package io.gunmetal.builder;

import com.github.overengineer.gunmetal.scope.Scope;

/**
 * @author rees.byars
 */
interface ComponentAdapter<T> extends ProvisionStrategy<T>, AccessFilter<DependencyRequest> {

    Class<T> getComponentClass();

    ModuleAdapter getModuleAdapter();

    Scope getScope();

    CompositeQualifier getCompositeQualifier();

}
