package io.gunmetal.internal;

import io.gunmetal.Dependency;

/**
 * @author rees.byars
 */
interface Binder {

    <T> ComponentAdapter<T> bind(Dependency<?> dependency, ComponentAdapter<T> componentAdapter);

}
