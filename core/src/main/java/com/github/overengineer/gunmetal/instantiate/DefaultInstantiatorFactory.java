package com.github.overengineer.gunmetal.instantiate;

import com.github.overengineer.gunmetal.parameter.ParameterBuilderFactory;
import com.github.overengineer.gunmetal.util.ConstructorProxy;

/**
 * @author rees.byars
 */
public class DefaultInstantiatorFactory implements InstantiatorFactory {

    private final ConstructorResolver constructorResolver;
    private final ParameterBuilderFactory parameterBuilderFactory;

    public DefaultInstantiatorFactory(ConstructorResolver constructorResolver, ParameterBuilderFactory parameterBuilderFactory) {
        this.constructorResolver = constructorResolver;
        this.parameterBuilderFactory = parameterBuilderFactory;
    }

    @Override
    public <T> Instantiator<T> create(Class<T> implementationType, Class ... providedArgTypes) {
        ConstructorProxy<T> constructorProxy = constructorResolver.resolveConstructor(implementationType, providedArgTypes);
        return new DefaultInstantiator<T>(
                implementationType,
                constructorProxy,
                parameterBuilderFactory.create(implementationType, constructorProxy, providedArgTypes));
    }

}
