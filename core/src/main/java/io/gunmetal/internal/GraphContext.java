package io.gunmetal.internal;

import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.util.Generics;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author rees.byars
 */
interface GraphContext {

    ProvisionStrategyDecorator strategyDecorator();

    Linkers linkers();

    <T> T statefulSource(Class<T> sourceClass);

    Set<Class<?>> loadedModules();

    static GraphContext create(final ProvisionStrategyDecorator strategyDecorator,
                               final Linkers linkers,
                               final Map<Class<?>, Object> statefulSources,
                               final Set<Class<?>> parentLoadedModules) {

        final Set<Class<?>> loadedModules = new HashSet<>();
        loadedModules.addAll(parentLoadedModules);

        return new GraphContext() {

            @Override public ProvisionStrategyDecorator strategyDecorator() {
                return strategyDecorator;
            }
            @Override public Linkers linkers() {
                return linkers;
            }
            @Override public <T> T statefulSource(Class<T> sourceClass) {
                return Generics.as(statefulSources.get(sourceClass));
            }

            @Override public Set<Class<?>> loadedModules() {
                return loadedModules;
            }
        };

    }

}
