package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

import java.util.List;

/**
 * @author rees.byars
 */
public class DefaultComponentInjector<T> implements ComponentInjector<T> {

    private static final long serialVersionUID = 8249251919444493316L;
    private final List<FieldInjector<T>> fieldInjectors;
    private final List<MethodInjector<T>> methodInjectors;

    DefaultComponentInjector(List<FieldInjector<T>> fieldInjectors, List<MethodInjector<T>> methodInjectors) {
        this.fieldInjectors = fieldInjectors;
         this.methodInjectors = methodInjectors;
    }

    @Override
    public void inject(T component, InternalProvider provider, ResolutionContext resolutionContext) {
        //using indices instead of iterators to save on the iterator allocations
        int i = 0;
        for (; i < fieldInjectors.size(); i++) {
            fieldInjectors.get(i).inject(component, provider, resolutionContext);
        }
        for (i = 0; i < methodInjectors.size(); i++) {
            methodInjectors.get(i).inject(component, provider, resolutionContext);
        }
    }
}
