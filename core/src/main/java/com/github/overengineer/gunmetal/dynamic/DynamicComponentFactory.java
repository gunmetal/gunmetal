package com.github.overengineer.gunmetal.dynamic;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.Provider;
import com.github.overengineer.gunmetal.key.Dependency;

import java.io.Serializable;

/**
 * a FactoryFactory that removes the need for all other FactoryFactories
 *
 * @author rees.byars
 */
public interface DynamicComponentFactory extends Serializable {
    <T> T createManagedComponentFactory(Class factoryInterface, Dependency producedTypeKey, InternalProvider provider);
    <T> T createNonManagedComponentFactory(Class factoryInterface, Class concreteProducedType, InternalProvider provider);
    <T> T createCompositeHandler(Class<T> targetInterface, Provider provider);
    <T> T createDelegatingService(Class<T> serviceInterface, InternalProvider provider);
}
