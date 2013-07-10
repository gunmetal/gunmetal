package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ComponentInjector<T> extends Serializable {
    void inject(T component, InternalProvider provider, ResolutionContext resolutionContext);
}
