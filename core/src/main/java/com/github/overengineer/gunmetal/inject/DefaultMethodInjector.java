package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.Provider;
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
    public Object inject(T component, Provider provider, Object ... providedArgs) {
        return methodProxy.invoke(component, parameterBuilder.buildParameters(provider, providedArgs));
    }

}
