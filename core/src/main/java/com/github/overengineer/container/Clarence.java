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
import com.github.overengineer.container.metadata.FastMetadataAdapter;
import com.github.overengineer.container.metadata.Jsr330MetadataAdapter;
import com.github.overengineer.container.metadata.MetadataAdapter;
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
public class Clarence implements Serializable {

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
        parameterBuilderFactory = parameterBuilderFactory != null ? parameterBuilderFactory : new PrecedingArgsParameterBuilderFactory(metadataAdapter);
        injectorFactory = injectorFactory != null ? injectorFactory : new DefaultInjectorFactory(metadataAdapter, parameterBuilderFactory);
        constructorResolver = constructorResolver != null ? constructorResolver : new DefaultConstructorResolver(metadataAdapter);
        instantiatorFactory = instantiatorFactory != null ? instantiatorFactory : new DefaultInstantiatorFactory(constructorResolver, parameterBuilderFactory);
        initializationListeners = initializationListeners != null ? initializationListeners : new ArrayList<ComponentInitializationListener>();
        strategyFactory = strategyFactory != null ? strategyFactory :  new DefaultComponentStrategyFactory(metadataAdapter, injectorFactory, instantiatorFactory, initializationListeners);
        dynamicComponentFactory = dynamicComponentFactory != null ? dynamicComponentFactory : new DefaultDynamicComponentFactory(instantiatorFactory, injectorFactory, metadataAdapter);
        builder = new DefaultContainer(strategyFactory, dynamicComponentFactory, metadataAdapter, initializationListeners);
        return builder;
    }

    public static Clarence please() {
        return new Clarence();
    }

    public HotSwappableContainer gimmeThatProxyTainer() {
        return (HotSwappableContainer) makeYourStuffInjectable().getBuilder().loadModule(ProxyModule.class).get(HotSwappableContainer.class).addCascadingContainer(getBuilder());
    }

    public AopContainer gimmeThatAopTainer() {
        return (AopContainer) makeYourStuffInjectable().getBuilder().loadModule(AopModule.class).get(AopContainer.class).addCascadingContainer(getBuilder());
    }

    public Container gimmeThatTainer() {
        return getBuilder();
    }

    public Clarence withJsr330Metadata() {
        metadataAdapter = new Jsr330MetadataAdapter();
        return this;
    }

    public Clarence withFastMetadata() {
        //TODO the fluid api design is some ghetto shit, as illustrated by this method
        metadataAdapter = new FastMetadataAdapter();
        injectorFactory = new FalseInjectorFactory(new PrecedingArgsParameterBuilderFactory(metadataAdapter));
        return this;
    }

    public Clarence makeYourStuffInjectable() {
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
