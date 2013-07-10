package com.github.overengineer.gunmetal;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
public interface ResolutionContext {

    <T> ComponentStrategyContext<T> getStrategyContext(ComponentStrategy<T> strategy);

    interface State { }

    enum States implements State {
        NEW, PRE_INSTANTIATION, PRE_INJECTION
    }

    class ComponentStrategyContext<T> {
        State state = States.NEW;
        T component;
    }

    class Factory {

        private static class ResolutionContextImpl implements ResolutionContext {

            private final Map<ComponentStrategy, ComponentStrategyContext> contextMap = new HashMap<ComponentStrategy, ComponentStrategyContext>();

            @Override
            public <T> ComponentStrategyContext<T> getStrategyContext(ComponentStrategy<T> strategy) {

                @SuppressWarnings("unchecked")
                ComponentStrategyContext<T> strategyContext = (ComponentStrategyContext<T>) contextMap.get(strategy);

                if (strategyContext == null) {
                    strategyContext = new ComponentStrategyContext<T>();
                    contextMap.put(strategy, strategyContext);
                }

                return strategyContext;
            }
        }

        public static ResolutionContext create() {
            return new ResolutionContextImpl();
        }

    }
}
