package com.github.overengineer.container.inject;

import com.github.overengineer.container.Provider;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ComponentInjector<T> extends Serializable {
    void inject(T component, Provider provider);
}
