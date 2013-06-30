package com.github.overengineer.gunmetal.parameter;

import com.github.overengineer.gunmetal.Provider;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ParameterProxy<T> extends Serializable {

    T get(Provider provider);

}
