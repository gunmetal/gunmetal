package com.github.overengineer.gunmetal.instantiate;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;
import com.github.overengineer.gunmetal.parameter.ParameterBuilder;
import com.github.overengineer.gunmetal.util.ConstructorProxy;

/**
 * @author rees.byars
 */
public class DefaultInstantiator<T> implements Instantiator<T> {

    private static final long serialVersionUID = 3787040143095057204L;
    private final ConstructorProxy<T> constructorProxy;
    private final ParameterBuilder<T> parameterBuilder;

    DefaultInstantiator(ConstructorProxy<T> constructorProxy, ParameterBuilder<T> parameterBuilder) {
        this.constructorProxy = constructorProxy;
        this.parameterBuilder = parameterBuilder;
    }

    @Override
    public boolean isDecorator() {
        return parameterBuilder.isDecorator();
    }

    @Override
    public T getInstance(InternalProvider provider, ResolutionContext resolutionContext) {
        return constructorProxy.newInstance(parameterBuilder.buildParameters(provider, resolutionContext));
    }

    @Override
    public T getInstance(InternalProvider provider, ResolutionContext resolutionContext, Object ... providedArgs) {
        return constructorProxy.newInstance(parameterBuilder.buildParameters(provider, resolutionContext, providedArgs));
    }

    @Override
    public Class getProducedType() {
        return constructorProxy.getDeclaringClass();
    }

}
