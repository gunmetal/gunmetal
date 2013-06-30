package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.Provider;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface MethodInjector<T> extends Serializable {
    Object inject(T component, Provider provider, Object ... providedArgs);
}
