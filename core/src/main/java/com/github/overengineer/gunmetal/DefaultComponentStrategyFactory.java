package com.github.overengineer.gunmetal;

import com.github.overengineer.gunmetal.inject.ComponentInjector;
import com.github.overengineer.gunmetal.inject.InjectorFactory;
import com.github.overengineer.gunmetal.inject.MethodInjector;
import com.github.overengineer.gunmetal.instantiate.Instantiator;
import com.github.overengineer.gunmetal.instantiate.InstantiatorFactory;
import com.github.overengineer.gunmetal.metadata.MetadataAdapter;
import com.github.overengineer.gunmetal.scope.Scope;
import com.github.overengineer.gunmetal.scope.Scopes;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author rees.byars
 */
public class DefaultComponentStrategyFactory implements ComponentStrategyFactory {

    private static final long serialVersionUID = -3605896311361401709L;
    private final MetadataAdapter metadataAdapter;
    private final InjectorFactory injectorFactory;
    private final InstantiatorFactory instantiatorFactory;
    private final List<ComponentPostProcessor> postProcessors;

    public DefaultComponentStrategyFactory(MetadataAdapter metadataAdapter, InjectorFactory injectorFactory, InstantiatorFactory instantiatorFactory, List<ComponentPostProcessor> postProcessors) {
        this.metadataAdapter = metadataAdapter;
        this.injectorFactory = injectorFactory;
        this.instantiatorFactory = instantiatorFactory;
        this.postProcessors = postProcessors;
    }

    @Override
    public <T> ComponentStrategy<T> create(Class<T> implementationType, Object qualifier, Scope scope) {
        ComponentInjector<T> injector = injectorFactory.create(implementationType);
        Instantiator<T> instantiator = instantiatorFactory.create(implementationType);
        if (Scopes.UNDEFINED == scope) {
            scope = metadataAdapter.getScope(implementationType);
            if (scope == Scopes.UNDEFINED) {
                scope = metadataAdapter.getDefaultScope();
            }
        }
        if (Scopes.PROTOTYPE == scope) {
            return new PrototypeComponentStrategy<T>(injector, instantiator, qualifier, postProcessors);
        } else if (Scopes.SINGLETON == scope) {
            return new SingletonComponentStrategy<T>(new PrototypeComponentStrategy<T>(injector, instantiator, qualifier, postProcessors));
        } else {
            return metadataAdapter.getStrategyProvider(scope).get(implementationType, qualifier, new PrototypeComponentStrategy<T>(injector, instantiator, qualifier, postProcessors));
        }
    }

    @Override
    public <T> ComponentStrategy<T> createInstanceStrategy(T implementation, Object qualifier) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) implementation.getClass();
        ComponentInjector<T> injector = injectorFactory.create(clazz);
        return new InstanceStrategy<T>(implementation, injector, qualifier, postProcessors);
    }

    @Override
    public <T> ComponentStrategy<T> createCustomStrategy(ComponentStrategy providerStrategy, Object qualifier) {
        Method providerMethod = metadataAdapter.getCustomProviderMethod(providerStrategy.getComponentType());
        @SuppressWarnings("unchecked")
        MethodInjector<T> methodInjector = injectorFactory.create(providerStrategy.getComponentType(), providerMethod);
        return new CustomComponentStrategy<T>(providerStrategy, methodInjector, providerMethod.getReturnType(), qualifier);
    }

}
