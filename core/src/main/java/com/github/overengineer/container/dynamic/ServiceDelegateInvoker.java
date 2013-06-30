package com.github.overengineer.container.dynamic;

import com.github.overengineer.container.Provider;
import com.github.overengineer.container.inject.MethodInjector;
import com.github.overengineer.container.key.Dependency;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public class ServiceDelegateInvoker<T> implements Serializable {

    private final Dependency<T> serviceDelegateDependency;
    private final MethodInjector<T> methodInjector;
    private final Provider provider;

    ServiceDelegateInvoker(Dependency<T> serviceDelegateDependency, MethodInjector<T> methodInjector, Provider provider) {
        this.serviceDelegateDependency = serviceDelegateDependency;
        this.methodInjector = methodInjector;
        this.provider = provider;
    }

    public Object invoke(Object[] providedArgs) {
        return methodInjector.inject(provider.get(serviceDelegateDependency), provider, providedArgs);
    }
}
