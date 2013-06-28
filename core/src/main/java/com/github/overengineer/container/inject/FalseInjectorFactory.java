package com.github.overengineer.container.inject;

import com.github.overengineer.container.parameter.ParameterBuilderFactory;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
public class FalseInjectorFactory implements InjectorFactory {

    private final ParameterBuilderFactory parameterBuilderFactory;

    public FalseInjectorFactory(ParameterBuilderFactory parameterBuilderFactory) {
        this.parameterBuilderFactory = parameterBuilderFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ComponentInjector<T> create(Class<T> implementationType) {
        return new DefaultInjectorFactory.EmptyInjector<T>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> MethodInjector<T> create(Class<T> injectionTarget, Method method, Class ... providedArgs) {
        return new DefaultMethodInjector(method, parameterBuilderFactory.create(injectionTarget, method, providedArgs));
    }
}
