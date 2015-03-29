package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.spi.ResourceMetadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author rees.byars
 */
class ComponentContext {

    private final ProvisionStrategyDecorator strategyDecorator;
    private final Linkers linkers;
    private final Errors errors;
    private final Set<Class<?>> loadedModules = new HashSet<>();
    private final Map<Dependency, Object> statefulSources;

    ComponentContext(ProvisionStrategyDecorator strategyDecorator,
                     Linkers linkers,
                     Errors errors,
                     Map<Dependency, Object> statefulSources) {
        this.strategyDecorator = strategyDecorator;
        this.linkers = linkers;
        this.errors = errors;
        this.statefulSources = statefulSources;
    }

    ProvisionStrategyDecorator strategyDecorator() {
        return strategyDecorator;
    }

    Linkers linkers() {
        return linkers;
    }

    Errors errors() {
        return errors;
    }

    Set<Class<?>> loadedModules() {
        return loadedModules;
    }

    ResolutionContext newResolutionContext() {
        return new ResolutionContextImpl(statefulSources);
    }

    private static class ResolutionContextImpl implements ResolutionContext {

        private final Map<ResourceMetadata<?>, ProvisionContext> contextMap = new HashMap<>();
        private Map<Dependency, Object> params;

        ResolutionContextImpl(Map<Dependency, Object> statefulSources) {
            if (!statefulSources.isEmpty()) {
                params = new HashMap<>(statefulSources);
            }
        }

        @Override public ProvisionContext provisionContext(ResourceMetadata<?> resourceMetadata) {

            ProvisionContext strategyContext = contextMap.get(resourceMetadata);

            if (strategyContext == null) {
                strategyContext = new ProvisionContext();
                contextMap.put(resourceMetadata, strategyContext);
            }

            return strategyContext;
        }

        @Override public void setParam(Dependency dependency, Object value) {
            if (params == null) {
                params = new HashMap<>();
            }
            params.put(dependency, value);
        }

        @Override public Object getParam(Dependency dependency) {
            if (params == null) {
                return null;
            }
            return params.get(dependency);
        }

    }

}
