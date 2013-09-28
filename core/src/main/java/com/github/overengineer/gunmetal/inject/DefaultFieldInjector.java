package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.ComponentStrategy;
import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;
import com.github.overengineer.gunmetal.SelectionAdvisor;
import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.util.FieldProxy;

/**
 * @author rees.byars
 */
public class DefaultFieldInjector<T> implements FieldInjector<T> {

    private static final long serialVersionUID = 3765168230740501039L;
    private final FieldProxy fieldProxy;
    private final Dependency<?> dependency;
    private volatile ComponentStrategy strategy;

    DefaultFieldInjector(FieldProxy fieldProxy, Dependency<?> dependency) {
        this.fieldProxy = fieldProxy;
        this.dependency = dependency;
    }

    @Override
    public void inject(T component, InternalProvider provider, ResolutionContext resolutionContext) {
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
        fieldProxy.set(component, strategy.get(provider, resolutionContext));
    }

}
