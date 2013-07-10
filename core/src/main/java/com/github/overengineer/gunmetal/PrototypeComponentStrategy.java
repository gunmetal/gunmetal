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

    @Override
    public T get(InternalProvider provider, ResolutionContext resolutionContext) {
        ResolutionContext.ComponentStrategyContext<T> strategyContext = resolutionContext.getStrategyContext(this);
        if (strategyContext.state != ResolutionContext.States.NEW) {
            if (strategyContext.state == ResolutionContext.States.PRE_INJECTION) {
                return strategyContext.component;
            }
            throw new CircularReferenceException(getComponentType(), getQualifier());
        } else {
            strategyContext.state = ResolutionContext.States.PRE_INSTANTIATION;
        }
        try {
            strategyContext.component = instantiator.getInstance(provider, resolutionContext);
            strategyContext.state = ResolutionContext.States.PRE_INJECTION;
            injector.inject(strategyContext.component, provider, resolutionContext);
            for (ComponentInitializationListener listener : initializationListeners) {
                strategyContext.component = listener.onInitialization(strategyContext.component);
            }
            strategyContext.state = ResolutionContext.States.NEW;
            return strategyContext.component;
        } catch (CircularReferenceException e) {
            if (e.getComponentType() == getComponentType() && e.getQualifier() == getQualifier()) {
                strategyContext.state = ResolutionContext.States.NEW;
                ComponentStrategy<?> reverseStrategy = e.getReverseStrategy();
                reverseStrategy.get(provider, resolutionContext);
                return strategyContext.component;
            } else if ((e.getComponentType() != getComponentType() || e.getQualifier() != getQualifier()) && e.getReverseStrategy() == null) {
                e.setReverseStrategy(this);
            }
            strategyContext.state = ResolutionContext.States.NEW;
            throw e;
        }
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
