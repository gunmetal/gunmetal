package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;
import com.github.overengineer.gunmetal.parameter.ParameterBuilder;
import com.github.overengineer.gunmetal.util.MethodProxy;

/**
 * @author rees.byars
 */
public class DefaultMethodInjector<T> implements MethodInjector<T> {

    private final MethodProxy methodProxy;
    private final ParameterBuilder parameterBuilder;

    DefaultMethodInjector(MethodProxy methodProxy, ParameterBuilder<T> parameterBuilder) {
        this.methodProxy = methodProxy;
        this.parameterBuilder = parameterBuilder;
    }

    @Override
    public Object inject(T component, InternalProvider provider, ResolutionContext resolutionContext, Object ... providedArgs) {
        return methodProxy.invoke(component, parameterBuilder.buildParameters(provider, resolutionContext, providedArgs));
    }

}
