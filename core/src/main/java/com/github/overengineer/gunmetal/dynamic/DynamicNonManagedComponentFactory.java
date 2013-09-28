package com.github.overengineer.gunmetal.dynamic;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;
import com.github.overengineer.gunmetal.instantiate.Instantiator;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
public class DynamicNonManagedComponentFactory<T> implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 891084950495905266L;
    private final Class<T> factoryInterface;
    private final Class concreteProducedType;
    private final InternalProvider provider;
    private final Instantiator instantiator;
    T proxy;

    DynamicNonManagedComponentFactory(Class<T> factoryInterface, Class concreteProducedType, InternalProvider provider, Instantiator instantiator) {
        this.factoryInterface = factoryInterface;
        this.concreteProducedType = concreteProducedType;
        this.provider = provider;
        this.instantiator = instantiator;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        String methodName = method.getName();
        if ("equals".equals(methodName)) {
            return proxy == objects[0];
        } else if ("hashCode".equals(methodName)) {
            return System.identityHashCode(proxy);
        } else if ("toString".equals(methodName)) {
            return proxy.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + "$DynamicNonManagedComponentFactory$[" + factoryInterface.getName() + "][" + concreteProducedType.getName() + "]";
        }
        return instantiator.getInstance(provider, ResolutionContext.Factory.create(), objects);
    }
}
