package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.Provider;

import java.util.List;

/**
 * @author rees.byars
 */
public class DefaultComponentInjector<T> implements ComponentInjector<T> {

    private final List<FieldInjector<T>> fieldInjectors;
    private final List<MethodInjector<T>> methodInjectors;

    DefaultComponentInjector(List<FieldInjector<T>> fieldInjectors, List<MethodInjector<T>> methodInjectors) {
        this.fieldInjectors = fieldInjectors;
         this.methodInjectors = methodInjectors;
    }

    @Override
    public void inject(T component, Provider provider) {
        for (FieldInjector<T> injector : fieldInjectors) {
            injector.inject(component, provider);
        }
        for (MethodInjector<T> injector : methodInjectors) {
            injector.inject(component, provider);
        }
    }
}
