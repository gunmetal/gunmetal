package io.gunmetal.internal;

import io.gunmetal.spi.Errors;
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

    Errors errors();

    <T> T statefulSource(Class<T> sourceClass);

    static GraphContext create(final ProvisionStrategyDecorator strategyDecorator,
                               final Linkers linkers,
                               final Errors errors,
                               final Map<Class<?>, Object> statefulSources) {

        return new GraphContext() {

            @Override public ProvisionStrategyDecorator strategyDecorator() {
                return strategyDecorator;
            }

            @Override public Linkers linkers() {
                return linkers;
            }

            @Override public Errors errors() {
                return errors;
            }

            @Override public <T> T statefulSource(Class<T> sourceClass) {
                return Generics.as(statefulSources.get(sourceClass));
            }

        };

    }

}
