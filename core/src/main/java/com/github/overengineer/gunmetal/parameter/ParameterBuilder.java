package com.github.overengineer.gunmetal.parameter;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ParameterBuilder<T> extends Serializable {
    boolean isDecorator();
    Object[] buildParameters(InternalProvider provider, ResolutionContext resolutionContext);
    Object[] buildParameters(InternalProvider provider, ResolutionContext resolutionContext, Object[] providedArgs);
}
