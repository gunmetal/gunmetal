package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.CircularReferenceException;
import com.github.overengineer.gunmetal.ComponentStrategy;
import com.github.overengineer.gunmetal.Provider;
import com.github.overengineer.gunmetal.SelectionAdvisor;
import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.util.FieldProxy;

/**
 * @author rees.byars
 */
public class DefaultFieldInjector<T> implements FieldInjector<T> {

    private final FieldProxy fieldProxy;
    private final Dependency<?> dependency;
    private volatile ComponentStrategy strategy;

    DefaultFieldInjector(FieldProxy fieldProxy, Dependency<?> dependency) {
        this.fieldProxy = fieldProxy;
        this.dependency = dependency;
    }

    @Override
    public void inject(T component, Provider provider) {
        try {
            if (strategy == null) {
                synchronized (this) {
                    if (strategy == null && fieldProxy.isDecorated()) {
                        strategy = provider.getStrategy(dependency, new SelectionAdvisor() {
                            @Override
                            public boolean validSelection(ComponentStrategy<?> candidateStrategy) {
                                return candidateStrategy.getComponentType() != fieldProxy.getDeclaringClass();
                            }
                        });
                    } else if (strategy == null) {
                        strategy = provider.getStrategy(dependency, SelectionAdvisor.NONE);
                    }
                }
            }
            fieldProxy.set(component, strategy.get(provider));
        } catch (CircularReferenceException e) {
            e.setFieldProxy(fieldProxy);
            throw e;
        }
    }

}
