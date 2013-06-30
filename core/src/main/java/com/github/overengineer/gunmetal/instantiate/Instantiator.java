package com.github.overengineer.gunmetal.instantiate;

import com.github.overengineer.gunmetal.Provider;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface Instantiator<T> extends Serializable {

    boolean isDecorator();

    T getInstance(Provider provider, Object ... providedArgs);

    Class getProducedType();

}
