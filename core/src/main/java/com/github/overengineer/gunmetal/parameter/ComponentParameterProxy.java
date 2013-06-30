package com.github.overengineer.gunmetal.parameter;

import com.github.overengineer.gunmetal.Provider;
import com.github.overengineer.gunmetal.key.Dependency;

/**
 * @author rees.byars
 */
public class ComponentParameterProxy<T> implements ParameterProxy<T> {

    private final Dependency<T> dependency;

    ComponentParameterProxy(Dependency<T> dependency) {
        this.dependency = dependency;
    }

    @Override
    public T get(Provider provider) {
        return provider.get(dependency);
    }

}
