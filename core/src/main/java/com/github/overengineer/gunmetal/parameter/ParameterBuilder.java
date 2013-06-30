package com.github.overengineer.gunmetal.parameter;

import com.github.overengineer.gunmetal.Provider;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ParameterBuilder<T> extends Serializable {
    boolean isDecorator();
    Object[] buildParameters(Provider provider, Object[] providedArgs);
}
