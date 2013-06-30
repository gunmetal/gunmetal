package com.github.overengineer.gunmetal;

import com.github.overengineer.gunmetal.scope.Scope;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ComponentStrategyFactory extends Serializable {

    <T> ComponentStrategy<T> create(Class<T> implementationType, Object qualifier, Scope scope);

    <T> ComponentStrategy<T> createInstanceStrategy(T implementation, Object qualifier);

    <T> ComponentStrategy<T> createCustomStrategy(ComponentStrategy providerStrategy, Object qualifier);

}
