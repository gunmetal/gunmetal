package com.github.overengineer.gunmetal;

import com.github.overengineer.gunmetal.dynamic.DefaultDynamicComponentFactory;
import com.github.overengineer.gunmetal.dynamic.DynamicComponentFactory;
import com.github.overengineer.gunmetal.inject.DefaultInjectorFactory;
import com.github.overengineer.gunmetal.inject.FalseInjectorFactory;
import com.github.overengineer.gunmetal.inject.InjectorFactory;
import com.github.overengineer.gunmetal.instantiate.ConstructorResolver;
import com.github.overengineer.gunmetal.instantiate.DefaultConstructorResolver;
import com.github.overengineer.gunmetal.instantiate.InstantiatorFactory;
import com.github.overengineer.gunmetal.key.Generic;
import com.github.overengineer.gunmetal.metadata.DefaultMetadataAdapter;
import com.github.overengineer.gunmetal.metadata.Jsr330MetadataAdapter;
import com.github.overengineer.gunmetal.metadata.MetadataAdapter;
import com.github.overengineer.gunmetal.module.Module;
import com.github.overengineer.gunmetal.parameter.ParameterBuilderFactory;
import com.github.overengineer.gunmetal.parameter.PrecedingArgsParameterBuilderFactory;
import com.github.overengineer.gunmetal.proxy.HotSwappableContainer;
import com.github.overengineer.gunmetal.proxy.ProxyModule;
import com.github.overengineer.gunmetal.proxy.aop.AopContainer;
import com.github.overengineer.gunmetal.proxy.aop.AopModule;
import com.github.overengineer.gunmetal.instantiate.DefaultInstantiatorFactory;

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
    private List<ComponentPostProcessor> postProcessors;
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
        postProcessors = new ArrayList<ComponentPostProcessor>();
        strategyFactory = new DefaultComponentStrategyFactory(metadataAdapter, injectorFactory, instantiatorFactory, postProcessors);
        dynamicComponentFactory = new DefaultDynamicComponentFactory(instantiatorFactory, injectorFactory, metadataAdapter);
        builder = new DefaultContainer(strategyFactory, dynamicComponentFactory, metadataAdapter, postProcessors);
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
                .addInstance(new Generic<List<ComponentPostProcessor>>() {
                }, postProcessors);
        return this;
    }

}
