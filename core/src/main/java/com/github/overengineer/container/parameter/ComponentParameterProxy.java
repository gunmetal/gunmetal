package com.github.overengineer.container.parameter;

import com.github.overengineer.container.Provider;
import com.github.overengineer.container.key.Dependency;

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
