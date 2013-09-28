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
    private final List<ComponentPostProcessor> postProcessors;

    PrototypeComponentStrategy(ComponentInjector<T> injector, Instantiator<T> instantiator, Object qualifier, List<ComponentPostProcessor> postProcessors) {
        this.injector = injector;
        this.instantiator = instantiator;
        this.qualifier = qualifier;
        this.postProcessors = postProcessors;
    }

    @Override
    public T get(InternalProvider provider, ResolutionContext resolutionContext) {
        ResolutionContext.ComponentStrategyContext<T> strategyContext = resolutionContext.getStrategyContext(this);
        if (strategyContext.state != ResolutionContext.States.NEW) {
            if (strategyContext.state == ResolutionContext.States.PRE_INJECTION) {
                return strategyContext.component;
            }
            throw new CircularReferenceException(getComponentType(), getQualifier());
        }
        strategyContext.state = ResolutionContext.States.PRE_INSTANTIATION;
        try {
            strategyContext.component = instantiator.getInstance(provider, resolutionContext);
            strategyContext.state = ResolutionContext.States.PRE_INJECTION;
            injector.inject(strategyContext.component, provider, resolutionContext);
            //using index instead of iterator to save on the iterator allocation
            for (int i = 0; i < postProcessors.size(); i++) {
                strategyContext.component = postProcessors.get(i).postProcess(strategyContext.component);
            }
            strategyContext.state = ResolutionContext.States.NEW;
            return strategyContext.component;
        } catch (CircularReferenceException e) {
            strategyContext.state = ResolutionContext.States.NEW;
            if (e.getComponentType() == getComponentType() && e.getQualifier() == getQualifier()) {
                ComponentStrategy<?> reverseStrategy = e.getReverseStrategy();
                reverseStrategy.get(provider, resolutionContext);
                return strategyContext.component;
            } else if ((e.getComponentType() != getComponentType() || e.getQualifier() != getQualifier()) && e.getReverseStrategy() == null) {
                e.setReverseStrategy(this);
            }
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
