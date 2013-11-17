package io.gunmetal.internal;

import io.gunmetal.Component;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface ComponentAdapterFactory {

    <T> ComponentAdapter<T> create(Component component, ModuleAdapter moduleAdapter,
                                   AccessFilter<DependencyRequest> accessFilter,
                                   InternalProvider internalProvider);

    <T> ComponentAdapter<T> create(Method providerMethod, ModuleAdapter moduleAdapter,
                                   AccessFilter<DependencyRequest> accessFilter,
                                   InternalProvider internalProvider);

}
