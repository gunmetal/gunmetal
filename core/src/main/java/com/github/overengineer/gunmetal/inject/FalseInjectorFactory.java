package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.parameter.ParameterBuilderFactory;
import com.github.overengineer.gunmetal.util.MethodRefImpl;

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
        return new DefaultMethodInjector(new MethodRefImpl(method), parameterBuilderFactory.create(injectionTarget, method, providedArgs));
    }
}
