package com.github.overengineer.gunmetal;

import com.github.overengineer.gunmetal.dynamic.DynamicComponentFactory;
import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.key.Smithy;
import com.github.overengineer.gunmetal.key.TypeKey;
import com.github.overengineer.gunmetal.metadata.MetadataAdapter;
import com.github.overengineer.gunmetal.module.InstanceMapping;
import com.github.overengineer.gunmetal.module.Mapping;
import com.github.overengineer.gunmetal.module.Module;
import com.github.overengineer.gunmetal.scope.Scope;
import com.github.overengineer.gunmetal.scope.Scopes;
import com.github.overengineer.gunmetal.util.Order;
import com.github.overengineer.gunmetal.util.TypeRef;
import com.github.overengineer.gunmetal.key.Qualifier;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author rees.byars
 */
public class DefaultContainer implements Container {

    private final Map<TypeKey<?>, SortedSet<ComponentStrategy<?>>> strategies = new HashMap<TypeKey<?>, SortedSet<ComponentStrategy<?>>>();
    private final List<Container> cascadingContainers = new ArrayList<Container>();
    private final List<Container> children = new ArrayList<Container>();
    private final ComponentStrategyFactory strategyFactory;
    private final DynamicComponentFactory dynamicComponentFactory;
    private final MetadataAdapter metadataAdapter;
    private final List<ComponentInitializationListener> componentInitializationListeners;

    private final StrategyComparator strategyComparator = new StrategyComparator() {
        @Override
        public int compare(ComponentStrategy<?> strategy, ComponentStrategy<?> strategy2) {
            if (strategy.equals(strategy2)
                    //TODO need a better way to ensure only one composite/delegating service etc is allowed
                    || (strategy.getComponentType().equals(strategy2.getComponentType())
                    && strategy.getQualifier().equals(strategy2.getQualifier())
                    && !Proxy.isProxyClass(strategy.getComponentType()))) {
                return Order.EXCLUDE;
            } else if (strategy instanceof TopLevelStrategy) {
                return Order.PREPEND;
            } else if (strategy2 instanceof TopLevelStrategy) {
                return Order.APPEND;
            } else if (strategy.isDecorator()) {
                return Order.PREPEND;
            } else if (strategy2.isDecorator()) {
                return Order.APPEND;
            }
            return Order.PREPEND;
        }
    };

    public DefaultContainer(ComponentStrategyFactory strategyFactory, DynamicComponentFactory dynamicComponentFactory, MetadataAdapter metadataAdapter, List<ComponentInitializationListener> componentInitializationListeners) {
        this.strategyFactory = strategyFactory;
        this.dynamicComponentFactory = dynamicComponentFactory;
        this.metadataAdapter = metadataAdapter;
        this.componentInitializationListeners = componentInitializationListeners;
    }

    @Override
    public void verify() throws WiringException {
        try {
            for (SortedSet<ComponentStrategy<?>> strategySet : strategies.values()) {
                for (ComponentStrategy<?> strategy : strategySet) {
                    strategy.get(this);
                }
            }
        } catch (Exception e) {
            throw new WiringException("An exception occurred while verifying the container", e);
        }
        for (Container child : children) {
            child.verify();
        }
        for (Container cascader : cascadingContainers) {
            cascader.verify();
        }
    }

    @Override
    public <M extends Module> Container loadModule(M module) {
        module.configure();
        for (Mapping<?> mapping : module.getMappings()) {
            Class<?> implementationType = mapping.getImplementationType();
            Object qualifier = mapping.getQualifier();
            if (qualifier.equals(Qualifier.NONE)) {
                qualifier = metadataAdapter.getQualifier(implementationType, implementationType.getAnnotations());
            }
            if (mapping instanceof InstanceMapping) {
                InstanceMapping<?> instanceMapping = (InstanceMapping) mapping;
                Object instance = instanceMapping.getInstance();
                for (Class<?> target : mapping.getTargetClasses()) {
                    addMapping(Smithy.forge(target, qualifier), instance);
                }
                for (Dependency<?> targetGeneric : mapping.getTargetDependencies()) {
                    addMapping(targetGeneric, instance);
                }
            } else {
                for (Class<?> target : mapping.getTargetClasses()) {
                    addMapping(Smithy.forge(target, qualifier), implementationType, mapping.getScope());
                }
                for (Dependency<?> targetGeneric : mapping.getTargetDependencies()) {
                    addMapping(targetGeneric, implementationType, mapping.getScope());
                }
            }
        }
        for (Map.Entry<Dependency, Class> entry : module.getNonManagedComponentFactories().entrySet()) {
            registerNonManagedComponentFactory(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public <M extends Module> Container loadModule(Class<M> moduleClass) {
        return loadModule(strategyFactory.create(moduleClass, Qualifier.NONE, Scopes.PROTOTYPE).get(this));
    }

    @Override
    public synchronized Container addCascadingContainer(Container container) {
        if (this == container.getReal()) {
            throw new CircularReferenceException("Cannot add a container as a cascading container of itself");
        }
        if (isThisCascaderOfTarget(container)) {
            throw new CircularReferenceException("Cannot add a container as a cascader of one of its cascaders");
        }
        if (isTargetChildOfThis(container)) {
            throw new CircularReferenceException("Cannot add a child container as a cascader");
        }
        if (thisHasChildrenInCommonWithTarget(container)) {
            throw new CircularReferenceException("Cannot add a container as a cascader if the containers have children in common");
        }
        cascadingContainers.add(container);
        for (Container child : children) {
            child.addCascadingContainer(container);
        }
        return this;
    }

    @Override
    public synchronized Container addChild(Container child) {
        if (this == child.getReal()) {
            throw new CircularReferenceException("Cannot add a container as a child of itself");
        }
        if (isTargetCascaderOfThis(child)) {
            throw new CircularReferenceException("Cannot add a container as a child if it is already a cascader");
        }
        if (isThisCascaderOfTarget(child)) {
            throw new CircularReferenceException("Cannot add a container as a child of the one of the container's cascaders");
        }
        if (isThisChildOfTarget(child)) {
            throw new CircularReferenceException("Cannot add a container as a child of one of it's children");
        }
        children.add(child);
        for (Container cascadingContainer : cascadingContainers) {
            child.addCascadingContainer(cascadingContainer);
        }
        return this;
    }

    @Override
    public Container newEmptyClone() {
        return strategyFactory.create(this.getClass(), Qualifier.NONE, Scopes.SINGLETON).get(this);
    }

    @Override
    public Container addListener(Class<? extends ComponentInitializationListener> listenerClass) {
        ComponentStrategy strategy = strategyFactory.create(listenerClass, Qualifier.NONE, Scopes.SINGLETON);
        getInitializationListeners().add((ComponentInitializationListener) strategy.get(this));
        return this;
    }

    @Override
    public <T> Container add(Class<T> componentType, Class<? extends T> implementationType) {
        add(Smithy.forge(componentType, metadataAdapter.getQualifier(implementationType, implementationType.getAnnotations())), implementationType);
        return this;
    }

    @Override
    public <T> Container add(Class<T> componentType, Object qualifier, Class<? extends T> implementationType) {
        add(Smithy.forge(componentType, qualifier), implementationType);
        return this;
    }

    @Override
    public <T> Container add(Dependency<T> dependency, Class<? extends T> implementationType) {
        addMapping(dependency, implementationType, Scopes.UNDEFINED);
        return this;
    }

    @Override
    public <T, I extends T> Container addInstance(Class<T> componentType, I implementation) {
        addInstance(Smithy.forge(componentType, metadataAdapter.getQualifier(implementation.getClass(), implementation.getClass().getAnnotations())), implementation);
        return this;
    }

    @Override
    public <T, I extends T> Container addInstance(Class<T> componentType, Object qualifier, I implementation) {
        addInstance(Smithy.forge(componentType, qualifier), implementation);
        return this;
    }

    @Override
    public <T, I extends T> Container addInstance(Dependency<T> dependency, I implementation) {
        addMapping(dependency, implementation);
        return this;
    }

    @Override
    public Container addCustomProvider(Class<?> providedType, Class<?> customProviderType) {
        addCustomProvider(Smithy.forge(providedType, metadataAdapter.getQualifier(providedType, providedType.getAnnotations())), customProviderType);
        return this;
    }

    @Override
    public Container addCustomProvider(Dependency<?> providedTypeDependency, Class<?> customProviderType) {
        Object qualifier = providedTypeDependency.getQualifier();
        Dependency<?> providerDependency = Smithy.forge(customProviderType, qualifier);
        ComponentStrategy providerStrategy = tryGetStrategy(providerDependency, SelectionAdvisor.NONE);
        if (providerStrategy == null) {
            providerStrategy = strategyFactory.create(customProviderType, qualifier, Scopes.SINGLETON);
        }
        putStrategy(providerDependency, providerStrategy);
        putStrategy(providedTypeDependency, strategyFactory.createCustomStrategy(providerStrategy, qualifier));
        return this;
    }

    @Override
    public Container addCustomProvider(Class<?> providedType, Object customProvider) {
        addCustomProvider(Smithy.forge(providedType, metadataAdapter.getQualifier(providedType, providedType.getAnnotations())), customProvider);
        return this;
    }

    @Override
    public Container addCustomProvider(Dependency<?> providedTypeDependency, Object customProvider) {
        Object qualifier = providedTypeDependency.getQualifier();
        Dependency<?> providerDependency = Smithy.forge(customProvider.getClass(), qualifier);
        ComponentStrategy providerStrategy = tryGetStrategy(providerDependency, SelectionAdvisor.NONE);
        if (providerStrategy == null) {
            providerStrategy = strategyFactory.createInstanceStrategy(customProvider, qualifier);
        }
        putStrategy(providerDependency, providerStrategy);
        putStrategy(providedTypeDependency, strategyFactory.createCustomStrategy(providerStrategy, qualifier));
        return this;
    }

    @Override
    public Container registerNonManagedComponentFactory(Dependency<?> factoryDependency, Class producedType) {
        addMapping(factoryDependency, dynamicComponentFactory.createNonManagedComponentFactory(factoryDependency.getTypeKey().getRaw(), producedType, this));
        return this;
    }

    @Override
    public synchronized Container registerCompositeTarget(Class<?> targetInterface) {
        registerCompositeTarget(Smithy.forge(targetInterface));
        return this;
    }

    @Override
    public Container registerCompositeTarget(Class<?> targetInterface, Object qualifier) {
        registerCompositeTarget(Smithy.forge(targetInterface, qualifier));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized Container registerCompositeTarget(Dependency targetDependency) {
        Object composite = dynamicComponentFactory.createCompositeHandler(targetDependency.getTypeKey().getRaw(), this);
        ComponentStrategy compositeStrategy = new TopLevelStrategy(strategyFactory.createInstanceStrategy(composite, targetDependency.getQualifier()));
        putStrategy(targetDependency, compositeStrategy);
        return this;
    }

    @Override
    public Container registerDeconstructedApi(Class<?> targetInterface) {
        registerDeconstructedApi(Smithy.forge(targetInterface));
        return this;
    }

    @Override
    public Container registerDeconstructedApi(Class<?> targetInterface, Object qualifier) {
        registerDeconstructedApi(Smithy.forge(targetInterface, qualifier));
        return this;
    }

    @Override
    public Container registerDeconstructedApi(Dependency<?> targetDependency) {
        Object delegatingService = dynamicComponentFactory.createDelegatingService(targetDependency.getTypeKey().getRaw(), this);
        ComponentStrategy strategy = strategyFactory.createInstanceStrategy(delegatingService, targetDependency.getQualifier());
        putStrategy(targetDependency, strategy);
        return this;
    }

    @Override
    public List<ComponentInitializationListener> getInitializationListeners() {
        return componentInitializationListeners;
    }

    @Override
    public List<Object> getAllComponents() {
        List<Object> components = new LinkedList<Object>();
        components.addAll(getInitializationListeners());
        for (SortedSet<ComponentStrategy<?>> strategySet : strategies.values()) {
            for (ComponentStrategy<?> strategy : strategySet) {
                components.add(strategy.get(this));
            }
        }
        for (Container child : children) {
            components.addAll(child.getAllComponents());
        }
        return components;
    }

    @Override
    public List<Container> getCascadingContainers() {
        List<Container> result = new LinkedList<Container>(cascadingContainers);
        for (Container child : getChildren()) {
            result.addAll(child.getCascadingContainers());
        }
        for (Container cascader : cascadingContainers) {
            result.addAll(cascader.getChildren());
        }
        return result;
    }

    @Override
    public List<Container> getChildren() {
        List<Container> result = new LinkedList<Container>(children);
        for (Container child : children) {
            result.addAll(child.getChildren());
        }
        return result;
    }

    @Override
    public Container getReal() {
        return this;
    }

    @Override
    public Container makeInjectable() {
        addInstance(Container.class, this);
        addInstance(Provider.class, this);
        return this;
    }

    @Override
    public <T> T get(Class<T> clazz, SelectionAdvisor ... advisors) {
        return get(Smithy.forge(clazz), advisors);
    }

    @Override
    public <T> T get(Class<T> clazz, Object qualifier, SelectionAdvisor ... advisors) {
        return get(Smithy.forge(clazz, qualifier), advisors);
    }

    @Override
    public <T> T get(final Dependency<T> dependency, SelectionAdvisor ... advisors) {
        return getStrategy(dependency, advisors).get(this);
    }

    @Override
    public <T> List<T> getAll(Class<T> clazz, SelectionAdvisor... advisors) {
        return getAll(Smithy.forge(clazz), advisors);
    }

    @Override
    public <T> List<T> getAll(Class<T> clazz, Object qualifier, SelectionAdvisor... advisors) {
        return getAll(Smithy.forge(clazz, qualifier), advisors);
    }

    @Override
    public <T> List<T> getAll(Dependency<T> dependency, SelectionAdvisor... advisors) {
        List<T> components = new LinkedList<T>();
        List<ComponentStrategy<T>> componentStrategies = getAllStrategies(dependency, advisors);
        for (ComponentStrategy<T> strategy : componentStrategies) {
            components.add(strategy.get(this));
        }
        return components;
    }

    protected synchronized void addMapping(Dependency dependency, final Class<?> implementationType, Scope scope) {

        Dependency componentKey = Smithy.forge(implementationType, dependency.getQualifier());

        ComponentStrategy strategy = tryGetStrategy(componentKey, new SelectionAdvisor() {
            @Override
            public boolean validSelection(ComponentStrategy<?> candidateStrategy) {
                return candidateStrategy.getComponentType() == implementationType;
            }
        });

        if (strategy == null) {
            strategy = strategyFactory.create(implementationType, dependency.getQualifier(), scope);
            putStrategy(componentKey, strategy);
        }

        putStrategy(dependency, strategy);

    }

    protected synchronized void addMapping(Dependency dependency, Object implementation) {

        ComponentStrategy newStrategy = strategyFactory.createInstanceStrategy(implementation, dependency.getQualifier());
        putStrategy(dependency, newStrategy);
        putStrategy(Smithy.forge(implementation.getClass()), newStrategy);

    }

    @Override
    public <T> ComponentStrategy<T> getStrategy(final Dependency<T> dependency, SelectionAdvisor ... advisors) {


        ComponentStrategy<T> strategy = tryGetStrategy(dependency, advisors);

        if (strategy != null) {
            return strategy;
        }

        for (Container child : children) {
            try {
                return child.getStrategy(dependency, advisors);
            } catch (MissingDependencyException e) {
                //TODO this is shitty!!
            }
        }

        for (Container container : cascadingContainers) {
            try {
                return container.getStrategy(dependency, advisors);
            } catch (MissingDependencyException e) {
                //TODO this is shitty!!
            }
        }

        Class<?> targetClass = dependency.getTypeKey().getRaw();
        Type targetType = dependency.getTypeKey().getType();

        if (!targetClass.isInterface() && !Modifier.isAbstract(targetClass.getModifiers())) {
            addMapping(dependency, targetClass, Scopes.UNDEFINED);
            return getStrategy(dependency, advisors);
        }

        if (!(targetType instanceof ParameterizedType)) {
            throw new MissingDependencyException(dependency);
        }

        if (((ParameterizedType) targetType).getActualTypeArguments().length > 1) {
            throw new MissingDependencyException(dependency);
        }

        //TODO this is slow, refactor to cache the type in the key and to reuse the strategy
        //TODO everything after this this should probably also by synchronized :/

        Dependency parameterizedKey = Smithy.forge(new TypeRef() {
            @Override
            public Type getType() {
                return ((ParameterizedType) dependency.getTypeKey().getType()).getActualTypeArguments()[0];
            }
        }, dependency.getQualifier());

        if (metadataAdapter.getProviderClass().isAssignableFrom(targetClass)) {

            T instance = dynamicComponentFactory.createManagedComponentFactory(metadataAdapter.getProviderClass(), parameterizedKey, this);
            strategy = strategyFactory.createInstanceStrategy(instance, dependency.getQualifier());
            putStrategy(dependency, strategy);

            return strategy;

        }

        if (!(Collection.class.isAssignableFrom(targetClass))) {
            throw new MissingDependencyException(dependency);
        }

        if (List.class.isAssignableFrom(targetClass)) {

            @SuppressWarnings("unchecked")
            T t = (T) getAll(parameterizedKey);
            strategy = strategyFactory.createInstanceStrategy(t, dependency.getQualifier());
            putStrategy(dependency, strategy);
            return strategy;

        }

        if (Set.class.isAssignableFrom(targetClass)) {

            @SuppressWarnings("unchecked")
            T t = (T) new HashSet(getAll(parameterizedKey));
            strategy = strategyFactory.createInstanceStrategy(t, dependency.getQualifier());
            putStrategy(dependency, strategy);
            return strategy;

        }

        if (Collection.class == targetClass) {

            @SuppressWarnings("unchecked")
            T t = (T) getAll(parameterizedKey);
            strategy = strategyFactory.createInstanceStrategy(t, dependency.getQualifier());
            putStrategy(dependency, strategy);
            return strategy;

        }

        throw new MissingDependencyException(dependency);
    }

    private <T> ComponentStrategy<T> tryGetStrategy(Dependency<T> dependency, SelectionAdvisor ... advisors) {

        Object qualifier = dependency.getQualifier();
        boolean qualified = Qualifier.NONE != qualifier;

        SortedSet<ComponentStrategy<T>> strategySet = getStrategySet(dependency);
        if (strategySet != null) {
            for (ComponentStrategy<T> strategy : strategySet) {
                boolean valid = true;
                if (qualified && !qualifier.equals(strategy.getQualifier())) {
                    continue;
                }
                for (SelectionAdvisor advisor : advisors) {
                    if (!advisor.validSelection(strategy)) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    return strategy;
                }
            }
        }

        return null;
    }

    @Override
    public <T> List<ComponentStrategy<T>> getAllStrategies(final Dependency<T> dependency, SelectionAdvisor... advisors) {

        List<ComponentStrategy<T>> allStrategies = new LinkedList<ComponentStrategy<T>>();

        Object qualifier = dependency.getQualifier();
        boolean qualified = !Qualifier.NONE.equals(qualifier);

        SortedSet<ComponentStrategy<T>> strategySet = getStrategySet(dependency);
        if (strategySet != null) {
            for (ComponentStrategy<T> strategy : strategySet) {
                if (!qualified || qualifier.equals(strategy.getQualifier())) {
                    boolean valid = true;
                    for (SelectionAdvisor advisor : advisors) {
                        if (!advisor.validSelection(strategy)) {
                            valid = false;
                            break;
                        }
                    }
                    if (valid) {
                        allStrategies.add(strategy);
                    }
                }
            }
        }
        for (Container child : children) {
            List<ComponentStrategy<T>> childAllStrategies = child.getAllStrategies(dependency, advisors);
            allStrategies.addAll(childAllStrategies);
        }
        for (Container container : cascadingContainers) {
            List<ComponentStrategy<T>> containerAllStrategies = container.getAllStrategies(dependency, advisors);
            allStrategies.addAll(containerAllStrategies);
        }
        return allStrategies;
    }

    @SuppressWarnings("unchecked")
    protected <T> SortedSet<ComponentStrategy<T>> getStrategySet(Dependency<T> dependency) {
        SortedSet<ComponentStrategy<?>> strategySet = strategies.get(dependency.getTypeKey());
        return (SortedSet<ComponentStrategy<T>>) (SortedSet) strategySet;
    }

    protected void putStrategy(Dependency dependency, ComponentStrategy<?> strategy) {
        SortedSet<ComponentStrategy<?>> strategySet = strategies.get(dependency.getTypeKey());
        if (strategySet == null) {
            strategySet = new TreeSet<ComponentStrategy<?>>(strategyComparator);
            strategies.put(dependency.getTypeKey(), strategySet);
        }
        strategySet.add(strategy);
    }

    protected boolean isTargetCascaderOfThis(Container target) {
        for (Container cascader : getCascadingContainers()) {
            if (cascader.getReal() == target.getReal()) {
                return true;
            }
        }
        return false;
    }

    protected boolean isTargetChildOfThis(Container target) {
        for (Container child : getChildren()) {
            if (child.getReal() == target.getReal()) {
                return true;
            }
        }
        return false;
    }

    protected boolean isThisCascaderOfTarget(Container target) {
        for (Container cascader : target.getCascadingContainers()) {
            if (cascader.getReal() == this) {
                return true;
            }
        }
        return false;
    }

    protected boolean isThisChildOfTarget(Container target) {
        for (Container child : target.getChildren()) {
            if (child.getReal() == this) {
                return true;
            }
        }
        return false;
    }

    protected boolean thisHasChildrenInCommonWithTarget(Container target) {
        for (Container targetChild : target.getChildren()) {
            for (Container child : getChildren()) {
                if (targetChild.getReal() == child.getReal()) {
                    return true;
                }
            }
        }
        return false;
    }

}
