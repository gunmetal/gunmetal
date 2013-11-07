package io.gunmetal.builder;

import io.gunmetal.Component;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface ComponentAdapterFactory {

    <T> ComponentAdapter<T> create(Component component,
                                   AccessFilter<DependencyRequest> accessFilter,
                                   InternalProvider internalProvider);

    <T> ComponentAdapter<T> create(Method providerMethod,
                                   AccessFilter<DependencyRequest> accessFilter,
                                   InternalProvider internalProvider);

}
