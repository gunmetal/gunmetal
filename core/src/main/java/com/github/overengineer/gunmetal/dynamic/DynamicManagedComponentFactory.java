package com.github.overengineer.gunmetal.dynamic;

import com.github.overengineer.gunmetal.Provider;
import com.github.overengineer.gunmetal.key.Dependency;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
public class DynamicManagedComponentFactory<T> implements InvocationHandler, Serializable {

    private final Class<T> factoryInterface;
    private final Dependency<?> producedTypeDependency;
    private final Provider provider;
    T proxy;

    DynamicManagedComponentFactory(Class<T> factoryInterface, Dependency producedTypeDependency, Provider provider) {
        this.factoryInterface = factoryInterface;
        this.producedTypeDependency = producedTypeDependency;
        this.provider = provider;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        String methodName = method.getName();
        if ("equals".equals(methodName)) {
            return proxy == objects[0];
        } else if ("hashCode".equals(methodName)) {
            return System.identityHashCode(proxy);
        } else if ("toString".equals(methodName)) {
            return proxy.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + "$DynamicManagedComponentFactory$[" + factoryInterface + "][" + producedTypeDependency.getTypeKey().getType() + "]";
        }
        return provider.get(producedTypeDependency);
    }
}
