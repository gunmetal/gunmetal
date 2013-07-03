package com.github.overengineer.gunmetal.parameter;

import com.github.overengineer.gunmetal.ComponentStrategy;
import com.github.overengineer.gunmetal.Provider;
import com.github.overengineer.gunmetal.SelectionAdvisor;
import com.github.overengineer.gunmetal.key.Dependency;

/**
 * @author rees.byars
 */
public class DecoratorParameterProxy<T> implements ParameterProxy<T> {

    private final Dependency<T> dependency;
    private final Class<?> injectionTarget;
    private volatile ComponentStrategy<T> strategy;

    DecoratorParameterProxy(Dependency<T> dependency, Class<?> injectionTarget) {
        this.dependency = dependency;
        this.injectionTarget = injectionTarget;
    }

    @Override
    public T get(Provider provider) {
        if (strategy == null) {
            synchronized (this) {
                if (strategy == null) {
                    strategy = provider.getStrategy(dependency, new SelectionAdvisor() {
                        @Override
                        public boolean validSelection(ComponentStrategy<?> candidateStrategy) {
                            return candidateStrategy.getComponentType() != injectionTarget; //TODO this prevents self injection.  OK??
                        }
                    });
                }
            }
        }
        return strategy.get(provider);
    }

}