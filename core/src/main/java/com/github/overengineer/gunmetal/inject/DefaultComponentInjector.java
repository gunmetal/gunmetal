package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.Provider;

import java.util.List;

/**
 * @author rees.byars
 */
public class DefaultComponentInjector<T> implements ComponentInjector<T> {

    private final List<MethodInjector<T>> injectors;

    DefaultComponentInjector(List<MethodInjector<T>> injectors) {
         this.injectors = injectors;
    }

    @Override
    public void inject(T component, Provider provider) {
        for (MethodInjector<T> injector : injectors) {
            injector.inject(component, provider);
        }
    }
}
