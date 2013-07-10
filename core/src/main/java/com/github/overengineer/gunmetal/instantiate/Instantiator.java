package com.github.overengineer.gunmetal.instantiate;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface Instantiator<T> extends Serializable {

    boolean isDecorator();

    T getInstance(InternalProvider provider, ResolutionContext resolutionContext, Object ... providedArgs);

    Class getProducedType();

}
