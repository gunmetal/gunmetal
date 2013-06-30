package com.github.overengineer.gunmetal.proxy;

import com.github.overengineer.gunmetal.Container;
import com.github.overengineer.gunmetal.ComponentInitializationListener;
import com.github.overengineer.gunmetal.ComponentStrategy;
import com.github.overengineer.gunmetal.ComponentStrategyFactory;
import com.github.overengineer.gunmetal.DefaultContainer;
import com.github.overengineer.gunmetal.dynamic.DynamicComponentFactory;
import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.key.Smithy;
import com.github.overengineer.gunmetal.metadata.MetadataAdapter;
import com.github.overengineer.gunmetal.scope.Scopes;

import java.util.List;

/**
 * TODO the hot swapping doesnt use qualifiers
 *
 * @author rees.byars
 */
public class DefaultHotSwappableContainer extends DefaultContainer implements HotSwappableContainer {

    private final ComponentStrategyFactory strategyFactory;

    public DefaultHotSwappableContainer(ComponentStrategyFactory strategyFactory, DynamicComponentFactory dynamicComponentFactory, MetadataAdapter metadataAdapter, List<ComponentInitializationListener> componentInitializationListeners) {
        super(strategyFactory, dynamicComponentFactory, metadataAdapter, componentInitializationListeners);
        this.strategyFactory = strategyFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized  <T> void swap(Class<T> target, Class<? extends T> implementationType) throws HotSwapException {

        Dependency dependency = Smithy.forge(target);

        ComponentStrategy<T> currentStrategy = (ComponentStrategy<T>) getStrategy(dependency);

        if (!(currentStrategy instanceof HotSwappableProxyStrategy)) {
            throw new HotSwapException(target, currentStrategy.getComponentType(), implementationType);
        }

        ComponentProxyHandler<T> proxyHandler = ((HotSwappableProxyStrategy) currentStrategy).getProxyHandler();

        ComponentStrategy<T> newStrategy = (ComponentStrategy<T>) strategyFactory.create(implementationType, dependency.getQualifier(), Scopes.SINGLETON);

        if (!(newStrategy instanceof HotSwappableProxyStrategy)) {
            throw new HotSwapException(target, newStrategy.getComponentType(), implementationType);
        }

        ((HotSwappableProxyStrategy) newStrategy).swap(proxyHandler, this);

        for (Container child : getChildren()) {
            if (child instanceof HotSwappableContainer) {
                ((HotSwappableContainer) child).swap(target, implementationType);
            }
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized  <T, I extends T> void swap(Class<T> target, I implementation) throws HotSwapException {

        Dependency dependency = Smithy.forge(target);

        ComponentStrategy<T> currentStrategy = (ComponentStrategy<T>) getStrategy(dependency);

        if (!(currentStrategy instanceof HotSwappableProxyStrategy)) {
            throw new HotSwapException(target, currentStrategy.getComponentType(), implementation.getClass());
        }

        ComponentProxyHandler<T> proxyHandler = ((HotSwappableProxyStrategy) currentStrategy).getProxyHandler();

        ComponentStrategy<T> newStrategy = (ComponentStrategy<T>) strategyFactory.createInstanceStrategy(implementation, dependency.getQualifier());

        if (!(newStrategy instanceof HotSwappableProxyStrategy)) {
            throw new HotSwapException(target, newStrategy.getComponentType(), implementation.getClass());
        }

        ((HotSwappableProxyStrategy) newStrategy).swap(proxyHandler, this);

        for (Container child : getChildren()) {
            if (child instanceof HotSwappableContainer) {
                ((HotSwappableContainer) child).swap(target, implementation);
            }
        }

    }

    @Override
    public Container makeInjectable() {
        addInstance(HotSwappableContainer.class, this);
        return super.makeInjectable();
    }
}
