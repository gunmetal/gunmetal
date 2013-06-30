package com.github.overengineer.container;

import com.github.overengineer.container.key.Dependency;
import com.github.overengineer.container.module.Module;

import java.util.List;

/**
 * @author rees.byars
 */
public interface Container extends Provider {

    void verify() throws WiringException;

    <M extends Module> Container loadModule(M module);

    <M extends Module> Container loadModule(Class<M> module);

    Container addCascadingContainer(Container container);

    Container addChild(Container container);

    Container newEmptyClone();

    Container addListener(Class<? extends ComponentInitializationListener> listenerClass);

    <T> Container add(Class<T> componentType, Class<? extends T> implementationType);

    <T> Container add(Class<T> componentType, Object qualifier, Class<? extends T> implementationType);

    <T> Container add(Dependency<T> dependency, Class<? extends T> implementationType);

    <T, I extends T> Container addInstance(Class<T> componentType, I implementation);

    <T, I extends T> Container addInstance(Class<T> componentType, Object qualifier, I implementation);

    <T, I extends T> Container addInstance(Dependency<T> dependency, I implementation);

    Container addCustomProvider(Class<?> providedType, Class<?> customProviderType);

    Container addCustomProvider(Dependency<?> providedTypeDependency, Class<?> customProviderType);

    Container addCustomProvider(Class<?> providedType, Object customProvider);

    Container addCustomProvider(Dependency<?> providedTypeDependency, Object customProvider);

    Container registerNonManagedComponentFactory(Dependency<?> factoryDependency, Class producedType);

    Container registerCompositeTarget(Class<?> targetInterface);

    Container registerCompositeTarget(Class<?> targetInterface, Object qualifier);

    Container registerCompositeTarget(Dependency<?> targetDependency);

    Container registerDeconstructedApi(Class<?> targetInterface);

    Container registerDeconstructedApi(Class<?> targetInterface, Object qualifier);

    Container registerDeconstructedApi(Dependency<?> targetDependency);

    List<ComponentInitializationListener> getInitializationListeners();

    List<Object> getAllComponents();

    List<Container> getCascadingContainers();

    List<Container> getChildren();

    Container getReal();

    <T> ComponentStrategy<T> getStrategy(Dependency<T> key, SelectionAdvisor ... advisors);

    <T> List<ComponentStrategy<T>> getAllStrategies(Dependency<T> dependency, SelectionAdvisor... advisors);

    Container makeInjectable();

}
