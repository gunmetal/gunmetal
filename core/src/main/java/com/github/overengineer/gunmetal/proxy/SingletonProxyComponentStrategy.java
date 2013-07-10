package com.github.overengineer.gunmetal.proxy;

import com.github.overengineer.gunmetal.ComponentStrategy;
import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

/**
 * @author rees.byars
 */
public class SingletonProxyComponentStrategy<T> implements HotSwappableProxyStrategy<T> {

    private volatile ComponentProxyHandler<T> proxyHandler;
    private final Class<?> type;
    private final ComponentStrategy<T> delegateStrategy;
    private final ProxyHandlerFactory handlerFactory;

    SingletonProxyComponentStrategy(Class<?> type, ComponentStrategy<T> delegateStrategy, ProxyHandlerFactory handlerFactory) {
        this.type = type;
        this.delegateStrategy = delegateStrategy;
        this.handlerFactory = handlerFactory;
    }

    @Override
    public T get(InternalProvider provider, ResolutionContext resolutionContext) {

        if (proxyHandler == null) {

            synchronized (this) {

                if (proxyHandler == null) {

                    proxyHandler = handlerFactory.createProxy(type);

                    T component = delegateStrategy.get(provider, resolutionContext);

                    proxyHandler.setComponent(component);

                }

            }

        }

        return proxyHandler.getProxy();

    }

    @Override
    public Class getComponentType() {
        return delegateStrategy.getComponentType();
    }

    @Override
    public ComponentProxyHandler<T> getProxyHandler() {
        return proxyHandler;
    }

    @Override
    public void swap(ComponentProxyHandler<T> proxyHandler, InternalProvider provider , ResolutionContext resolutionContext) {

        this.proxyHandler = proxyHandler;

        T component = delegateStrategy.get(provider, resolutionContext);

        proxyHandler.setComponent(component);

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
