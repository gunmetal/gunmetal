package io.gunmetal.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
public interface ResolutionContext {

    <T> ProvisionContext<T> getProvisionContext(ProvisionStrategy<T> strategy);

    interface States  {
        byte NEW = 0;
        byte PRE_INSTANTIATION = 1;
        byte PRE_INJECTION = 2;
    }

    class ProvisionContext<T> {
        byte state = States.NEW;
        T component;
    }

    class Factory {

        private static class ResolutionContextImpl implements ResolutionContext {

            private final Map<ProvisionStrategy, ProvisionContext> contextMap = new HashMap<ProvisionStrategy, ProvisionContext>();

            @Override
            public <T> ProvisionContext<T> getProvisionContext(ProvisionStrategy<T> strategy) {

                @SuppressWarnings("unchecked")
                ProvisionContext<T> strategyContext = (ProvisionContext<T>) contextMap.get(strategy);

                if (strategyContext == null) {
                    strategyContext = new ProvisionContext<T>();
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
