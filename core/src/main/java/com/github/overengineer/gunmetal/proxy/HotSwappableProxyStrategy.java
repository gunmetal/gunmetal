package com.github.overengineer.gunmetal.proxy;

import com.github.overengineer.gunmetal.ComponentStrategy;
import com.github.overengineer.gunmetal.Provider;

/**
 * @author rees.byars
 */
public interface HotSwappableProxyStrategy<T> extends ComponentStrategy<T> {

    ComponentProxyHandler<T> getProxyHandler();

    void swap(ComponentProxyHandler<T> proxyHandler, Provider provider);

}
