package com.github.overengineer.gunmetal.scope;

import com.github.overengineer.gunmetal.ComponentStrategy;

import java.io.Serializable;

/**
 * @rees.byars
 */
public interface ScopedComponentStrategyProvider extends Serializable {
    <T> ComponentStrategy<T> get(Class<T> implementationType, Object qualifier, ComponentStrategy<T> delegateStrategy);
}
