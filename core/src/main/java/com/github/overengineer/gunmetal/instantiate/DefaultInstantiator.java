package com.github.overengineer.gunmetal.instantiate;

import com.github.overengineer.gunmetal.Provider;
import com.github.overengineer.gunmetal.parameter.ParameterBuilder;
import com.github.overengineer.gunmetal.util.ConstructorProxy;

/**
 * @author rees.byars
 */
public class DefaultInstantiator<T> implements Instantiator<T> {

    private final Class<T> type;
    private final ConstructorProxy<T> constructorProxy;
    private final ParameterBuilder<T> parameterBuilder;

    DefaultInstantiator(Class<T> type, ConstructorProxy<T> constructorProxy, ParameterBuilder<T> parameterBuilder) {
        this.type = type;
        this.constructorProxy = constructorProxy;
        this.parameterBuilder = parameterBuilder;
    }

    @Override
    public boolean isDecorator() {
        return parameterBuilder.isDecorator();
    }

    @Override
    public T getInstance(Provider provider, Object ... providedArgs) {
        return constructorProxy.newInstance(parameterBuilder.buildParameters(provider, providedArgs));
    }

    @Override
    public Class getProducedType() {
        return type;
    }

}
