package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

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
    public void inject(T component, InternalProvider provider, ResolutionContext resolutionContext) {
        for (FieldInjector<T> injector : fieldInjectors) {
            injector.inject(component, provider, resolutionContext);
        }
        for (MethodInjector<T> injector : methodInjectors) {
            injector.inject(component, provider, resolutionContext);
        }
    }
}
