package com.github.overengineer.gunmetal.parameter;

import com.github.overengineer.gunmetal.util.ParameterizedFunction;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ParameterBuilderFactory extends Serializable {

    <T> ParameterBuilder<T> create(Class<T> injectionTarget, ParameterizedFunction parameterizedFunction, Class[] providedArgs);

}
