package io.gunmetal.internal;

import io.gunmetal.spi.Errors;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.util.Generics;

import java.util.Map;

/**
 * @author rees.byars
 */
class GraphContext {

    private final ProvisionStrategyDecorator strategyDecorator;
    private final Linkers linkers;
    private final Errors errors;
    private final Map<Class<?>, Object> statefulSources;

    GraphContext(ProvisionStrategyDecorator strategyDecorator,
                 Linkers linkers,
                 Errors errors,
                 Map<Class<?>, Object> statefulSources) {
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

    <T> T statefulSource(Class<T> sourceClass) {
        return Generics.as(statefulSources.get(sourceClass));
    }

}
