package com.github.overengineer.gunmetal.proxy;

import com.github.overengineer.gunmetal.ComponentStrategy;
import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

/**
 * @author rees.byars
 */
public class PrototypeProxyComponentStrategy<T> implements ComponentStrategy<T> {

    private final Class<?> type;
    private final ComponentStrategy<T> delegateStrategy;
    private final ProxyHandlerFactory handlerFactory;

    PrototypeProxyComponentStrategy(Class<?> type, ComponentStrategy<T> delegateStrategy, ProxyHandlerFactory handlerFactory) {
        this.type = type;
        this.delegateStrategy = delegateStrategy;
        this.handlerFactory = handlerFactory;
    }

    @Override
    public T get(InternalProvider provider, ResolutionContext resolutionContext) {

        ComponentProxyHandler<T> proxyHandler = handlerFactory.createProxy(type);

        T component = delegateStrategy.get(provider, resolutionContext);

        proxyHandler.setComponent(component);

        return proxyHandler.getProxy();

    }

    @Override
    public Class getComponentType() {
        return delegateStrategy.getComponentType();
    }

    @Override
    public boolean isDecorator() {
        return delegateStrategy.isDecorator();
    }

    @Override
    public Object getQualifier() {
        return delegateStrategy.getQualifier();
    }

}
