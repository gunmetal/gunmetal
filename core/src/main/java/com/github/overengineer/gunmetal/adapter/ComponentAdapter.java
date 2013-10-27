package com.github.overengineer.gunmetal.adapter;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

/**
 * @author rees.byars
 */
public interface ComponentAdapter<T> extends VisibilityAdapter<ComponentAdapter> {
    T get(InternalProvider provider, ResolutionContext resolutionContext);
    Class<T> getComponentClass();
    ModuleAdapter getModuleAdapter();
    QualifierAdapter getQualifierAdapter();
}
