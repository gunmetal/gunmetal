package io.gunmetal.internal;

import io.gunmetal.Dependency;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
interface Bindings {

    <T> ComponentAdapter<T> add(Dependency<?> dependency, ComponentAdapter<T> componentAdapter);

    <T> ComponentAdapter<T> get(Dependency<T> dependency);

    final class Factory {

        private Factory() { }

        static Bindings create() {

            return new Bindings() {

                Map<Dependency<?>, ComponentAdapter<?>> bindingsMap = new HashMap<Dependency<?>, ComponentAdapter<?>>();

                @SuppressWarnings("unchecked")
                @Override
                public <T> ComponentAdapter<T> add(Dependency<?> dependency, ComponentAdapter<T> componentAdapter) {
                    // TODO perform type casting validation
                    return (ComponentAdapter<T>) bindingsMap.put(dependency, componentAdapter);
                }

                @SuppressWarnings("unchecked")
                @Override
                public <T> ComponentAdapter<T> get(Dependency<T> dependency) {
                    return (ComponentAdapter<T>) bindingsMap.get(dependency);
                }

            };

        }

    }
}
