package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface MethodInjector<T> extends Serializable {
    Object inject(T component, InternalProvider provider, ResolutionContext resolutionContext, Object ... providedArgs);
}
