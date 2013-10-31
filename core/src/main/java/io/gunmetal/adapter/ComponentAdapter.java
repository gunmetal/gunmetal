package io.gunmetal.adapter;

import com.github.overengineer.gunmetal.scope.Scope;
import io.gunmetal.builder.ProvisionStrategy;

/**
 * @author rees.byars
 */
public interface ComponentAdapter<T> extends VisibilityAdapter<ComponentAdapter>, ProvisionStrategy<T> {

    // TODO instead of making visibility adapter accessible, add Dependency to ResolutionContext,
    // TODO and add requesting ComponentAdapter to Dependency?

    Class<T> getComponentClass();
    ModuleAdapter getModuleAdapter();
    QualifierAdapter getQualifierAdapter();
    Scope getScope();
}
