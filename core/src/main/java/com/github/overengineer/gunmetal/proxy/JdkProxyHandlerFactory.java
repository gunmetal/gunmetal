package com.github.overengineer.gunmetal.proxy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
public class JdkProxyHandlerFactory implements ProxyHandlerFactory {

    private static final long serialVersionUID = -906764935370910993L;
    private final Map<Class<?>, JdkProxyFactory> proxyFactories = new HashMap<Class<?>, JdkProxyFactory>();

    @Override
    public <T> ComponentProxyHandler<T> createProxy(Class<?> targetClass) {
        JdkProxyFactory proxyFactory = proxyFactories.get(targetClass);
        if (proxyFactory == null) {
            proxyFactory = new DefaultJdkProxyFactory(targetClass);
            proxyFactories.put(targetClass, proxyFactory);
        }
        return new JdkComponentProxyHandler<T>(proxyFactory);
    }

}
