package com.github.overengineer.gunmetal.instantiate;

import com.github.overengineer.gunmetal.Provider;
import com.github.overengineer.gunmetal.inject.InjectionException;
import com.github.overengineer.gunmetal.parameter.ParameterBuilder;
import com.github.overengineer.gunmetal.util.ConstructorRef;

/**
 * @author rees.byars
 */
public class DefaultInstantiator<T> implements Instantiator<T> {

    private final Class<T> type;
    private final ConstructorRef<T> constructorRef;
    private final ParameterBuilder<T> parameterBuilder;

    DefaultInstantiator(Class<T> type, ConstructorRef<T> constructorRef, ParameterBuilder<T> parameterBuilder) {
        this.type = type;
        this.constructorRef = constructorRef;
        this.parameterBuilder = parameterBuilder;
    }

    @Override
    public boolean isDecorator() {
        return parameterBuilder.isDecorator();
    }

    @Override
    public T getInstance(Provider provider, Object ... providedArgs) {
        try {
            return constructorRef.getConstructor().newInstance(parameterBuilder.buildParameters(provider, providedArgs));
        } catch (Exception e) {
            throw new InjectionException("Could not create new instance of type [" + type.getName() + "]", e);
        }
    }

    @Override
    public Class getProducedType() {
        return type;
    }

}
