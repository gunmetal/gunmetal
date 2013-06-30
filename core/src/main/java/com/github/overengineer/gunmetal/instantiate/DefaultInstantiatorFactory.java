package com.github.overengineer.gunmetal.instantiate;

import com.github.overengineer.gunmetal.parameter.ParameterBuilderFactory;
import com.github.overengineer.gunmetal.util.ConstructorRef;

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
        ConstructorRef<T> constructorRef = constructorResolver.resolveConstructor(implementationType, providedArgTypes);
        return new DefaultInstantiator<T>(
                implementationType,
                constructorRef,
                parameterBuilderFactory.create(implementationType, constructorRef.getConstructor(), providedArgTypes));
    }

}
