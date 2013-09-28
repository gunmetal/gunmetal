package com.github.overengineer.gunmetal.dynamic;

import com.github.overengineer.gunmetal.ComponentStrategy;
import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;
import com.github.overengineer.gunmetal.SelectionAdvisor;
import com.github.overengineer.gunmetal.key.Dependency;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
public class DynamicManagedComponentFactory<T> implements InvocationHandler, Serializable {

    private static final long serialVersionUID = -2962658880306788721L;
    private final Class<T> factoryInterface;
    private final Dependency<?> producedTypeDependency;
    private final InternalProvider provider;
    private final ComponentStrategy strategy;
    T proxy;

    DynamicManagedComponentFactory(Class<T> factoryInterface, Dependency<?> producedTypeDependency, InternalProvider provider) {
        this.factoryInterface = factoryInterface;
        this.producedTypeDependency = producedTypeDependency;
        this.provider = provider;
        this.strategy = provider.getStrategy(producedTypeDependency, SelectionAdvisor.NONE);
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
        return strategy.get(provider, ResolutionContext.Factory.create());
    }
}
