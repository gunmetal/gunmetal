package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.Provider;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ComponentInjector<T> extends Serializable {
    void inject(T component, Provider provider);
}
