package com.github.overengineer.gunmetal.dynamic;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;
import com.github.overengineer.gunmetal.SelectionAdvisor;
import com.github.overengineer.gunmetal.inject.MethodInjector;
import com.github.overengineer.gunmetal.key.Dependency;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public class ServiceDelegateInvoker<T> implements Serializable {

    private final Dependency<T> serviceDelegateDependency;
    private final MethodInjector<T> methodInjector;
    private final InternalProvider provider;

    ServiceDelegateInvoker(Dependency<T> serviceDelegateDependency, MethodInjector<T> methodInjector, InternalProvider provider) {
        this.serviceDelegateDependency = serviceDelegateDependency;
        this.methodInjector = methodInjector;
        this.provider = provider;
    }

    public Object invoke(Object[] providedArgs) {
        return methodInjector.inject(provider.get(serviceDelegateDependency, ResolutionContext.Factory.create(), SelectionAdvisor.NONE), provider, ResolutionContext.Factory.create(), providedArgs);
    }
}
