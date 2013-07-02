package com.github.overengineer.gunmetal;

import com.github.overengineer.gunmetal.inject.ComponentInjector;
import com.github.overengineer.gunmetal.instantiate.Instantiator;

import java.util.List;

/**
 * @author rees.byars
 */
public class PrototypeComponentStrategy<T> implements ComponentStrategy<T> {

    private final ComponentInjector<T> injector;
    private final Instantiator<T> instantiator;
    private final Object qualifier;
    private final List<ComponentInitializationListener> initializationListeners;

    PrototypeComponentStrategy(ComponentInjector<T> injector, Instantiator<T> instantiator, Object qualifier, List<ComponentInitializationListener> initializationListeners) {
        this.injector = injector;
        this.instantiator = instantiator;
        this.qualifier = qualifier;
        this.initializationListeners = initializationListeners;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(Provider provider) {
        T component = instantiator.getInstance(provider);
        injector.inject(component, provider);
        for (ComponentInitializationListener listener : initializationListeners) {
            component = listener.onInitialization(component);
        }
        return component;
    }

    @Override
    public Class getComponentType() {
        return instantiator.getProducedType();
    }

    @Override
    public boolean isDecorator() {
        return instantiator.isDecorator();
    }

    @Override
    public Object getQualifier() {
        return qualifier;
    }

}
