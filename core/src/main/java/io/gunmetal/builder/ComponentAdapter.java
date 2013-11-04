package io.gunmetal.builder;

import com.github.overengineer.gunmetal.scope.Scope;

/**
 * @author rees.byars
 */
interface ComponentAdapter<T> extends ProvisionStrategy<T> {

    Class<T> getComponentClass();

    Scope getScope();

    CompositeQualifier getCompositeQualifier();
}
