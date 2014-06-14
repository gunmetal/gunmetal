package io.gunmetal.internal;

import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.util.Generics;

import java.util.Map;

/**
 * @author rees.byars
 */
interface GraphContext {

    ProvisionStrategyDecorator strategyDecorator();

    Linkers linkers();

    <T> T getStatefulSource(Class<T> sourceClass);

    static GraphContext create(final ProvisionStrategyDecorator strategyDecorator,
                               final Linkers linkers,
                               final Map<Class<?>, Object> statefulSources) {
        return new GraphContext() {
            @Override public ProvisionStrategyDecorator strategyDecorator() {
                return strategyDecorator;
            }
            @Override public Linkers linkers() {
                return linkers;
            }
            @Override public <T> T getStatefulSource(Class<T> sourceClass) {
                return Generics.as(statefulSources.get(sourceClass));
            }
        };
    }

}
