package com.github.overengineer.gunmetal.parameter;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ParameterProxy<T> extends Serializable {

    T get(InternalProvider provider, ResolutionContext resolutionContext);

}
