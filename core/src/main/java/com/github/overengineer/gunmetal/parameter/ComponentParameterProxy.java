package com.github.overengineer.gunmetal.parameter;

import com.github.overengineer.gunmetal.ComponentStrategy;
import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;
import com.github.overengineer.gunmetal.SelectionAdvisor;
import com.github.overengineer.gunmetal.key.Dependency;

/**
 * @author rees.byars
 */
public class ComponentParameterProxy<T> implements ParameterProxy<T> {

    private static final long serialVersionUID = -1276921210275146880L;
    private final Dependency<T> dependency;
    private volatile ComponentStrategy<T> strategy;

    ComponentParameterProxy(Dependency<T> dependency) {
        this.dependency = dependency;
    }

    @Override
    public T get(InternalProvider provider, ResolutionContext resolutionContext) {
        if (strategy == null) {
            synchronized (this) {
                if (strategy == null) {
                    strategy = provider.getStrategy(dependency, SelectionAdvisor.NONE);
                }
            }
        }
        return strategy.get(provider, resolutionContext);
    }

}
