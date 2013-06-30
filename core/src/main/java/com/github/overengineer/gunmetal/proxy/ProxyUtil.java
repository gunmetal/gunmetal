package com.github.overengineer.gunmetal.proxy;

import java.lang.reflect.Proxy;

/**
 * @author rees.byars
 */
public class ProxyUtil {

    @SuppressWarnings("unchecked")
    public static <T> T getRealComponent(T proxy) {
        if (Proxy.isProxyClass(proxy.getClass())) {
            return getRealComponent(((ComponentProxyHandler<T>) Proxy.getInvocationHandler(proxy)).getComponent());
        }
        return proxy;
    }

}
