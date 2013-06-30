package com.github.overengineer.container;

import com.github.overengineer.container.dynamic.DefaultDynamicComponentFactory;
import com.github.overengineer.container.dynamic.DynamicComponentFactory;
import com.github.overengineer.container.inject.DefaultInjectorFactory;
import com.github.overengineer.container.inject.FalseInjectorFactory;
import com.github.overengineer.container.inject.InjectorFactory;
import com.github.overengineer.container.instantiate.ConstructorResolver;
import com.github.overengineer.container.instantiate.DefaultConstructorResolver;
import com.github.overengineer.container.instantiate.InstantiatorFactory;
import com.github.overengineer.container.key.Generic;
import com.github.overengineer.container.metadata.DefaultMetadataAdapter;
import com.github.overengineer.container.metadata.Jsr330MetadataAdapter;
import com.github.overengineer.container.metadata.MetadataAdapter;
import com.github.overengineer.container.module.Module;
import com.github.overengineer.container.parameter.ParameterBuilderFactory;
import com.github.overengineer.container.parameter.PrecedingArgsParameterBuilderFactory;
import com.github.overengineer.container.proxy.HotSwappableContainer;
import com.github.overengineer.container.proxy.ProxyModule;
import com.github.overengineer.container.proxy.aop.AopContainer;
import com.github.overengineer.container.proxy.aop.AopModule;
import com.github.overengineer.container.instantiate.DefaultInstantiatorFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author rees.byars
 */
public class Gunmetal implements Serializable {

    private boolean setterInjection = false;

    private MetadataAdapter metadataAdapter;
    private ParameterBuilderFactory parameterBuilderFactory;
    private InjectorFactory injectorFactory;
    private ConstructorResolver constructorResolver;
    private InstantiatorFactory instantiatorFactory;
    private List<ComponentInitializationListener> initializationListeners;
    private ComponentStrategyFactory strategyFactory;
    private DynamicComponentFactory dynamicComponentFactory;
    private Container builder;

    private Container getBuilder() {
        if (builder != null) {
            return builder;
        }
        metadataAdapter = metadataAdapter != null ? metadataAdapter : new DefaultMetadataAdapter();
        parameterBuilderFactory = new PrecedingArgsParameterBuilderFactory(metadataAdapter);
        injectorFactory = setterInjection ? new DefaultInjectorFactory(metadataAdapter, parameterBuilderFactory) : new FalseInjectorFactory(parameterBuilderFactory);
        constructorResolver = new DefaultConstructorResolver(metadataAdapter);
        instantiatorFactory = new DefaultInstantiatorFactory(constructorResolver, parameterBuilderFactory);
        initializationListeners = new ArrayList<ComponentInitializationListener>();
        strategyFactory = new DefaultComponentStrategyFactory(metadataAdapter, injectorFactory, instantiatorFactory, initializationListeners);
        dynamicComponentFactory = new DefaultDynamicComponentFactory(instantiatorFactory, injectorFactory, metadataAdapter);
        builder = new DefaultContainer(strategyFactory, dynamicComponentFactory, metadataAdapter, initializationListeners);
        return builder;
    }

    public static Gunmetal raw() {
        return new Gunmetal();
    }

    public static Container create(Module... modules) {
        Container container = new Gunmetal().getBuilder();
        for (Module module : modules) {
            container.loadModule(module);
        }
        return container;
    }

    public static Gunmetal jsr330() {
        Gunmetal gunMetal = new Gunmetal();
        gunMetal.metadataAdapter = new Jsr330MetadataAdapter();
        return gunMetal;
    }

    public HotSwappableContainer gimmeThatProxyTainer() {
        return (HotSwappableContainer) makeYourStuffInjectable().getBuilder().loadModule(new ProxyModule()).get(HotSwappableContainer.class).addCascadingContainer(getBuilder());
    }

    public AopContainer gimmeThatAopTainer() {
        return (AopContainer) makeYourStuffInjectable().getBuilder().loadModule(new AopModule()).get(AopContainer.class).addCascadingContainer(getBuilder());
    }

    public Container load(Module... modules) {
        Container container = getBuilder();
        for (Module module : modules) {
            container.loadModule(module);
        }
        return container;
    }

    public Gunmetal withSetterInjection() {
        setterInjection = true;
        return this;
    }

    public Gunmetal makeYourStuffInjectable() {
        getBuilder()
                .makeInjectable()
                .addInstance(MetadataAdapter.class, metadataAdapter)
                .addInstance(InjectorFactory.class, injectorFactory)
                .addInstance(ParameterBuilderFactory.class, parameterBuilderFactory)
                .addInstance(ConstructorResolver.class, constructorResolver)
                .addInstance(InstantiatorFactory.class, instantiatorFactory)
                .addInstance(DynamicComponentFactory.class, dynamicComponentFactory)
                .addInstance(ComponentStrategyFactory.class, strategyFactory)
                .addInstance(new Generic<List<ComponentInitializationListener>>() {
                }, initializationListeners);
        return this;
    }

}
