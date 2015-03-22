package io.gunmetal.internal;

import io.gunmetal.Module;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResolutionContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rees.byars
 */
class GraphInjectorProvider implements Replicable<GraphInjectorProvider> {

    private final Map<Class<?>, Injector> injectors = new ConcurrentHashMap<>(1, .75f, 4);
    private final Map<Class<?>, Instantiator> instantiators = new ConcurrentHashMap<>(0, .75f, 4);
    private final InjectorFactory injectorFactory;
    private final ConfigurableMetadataResolver metadataResolver;
    private final GraphInjectorProvider parentProvider;

    GraphInjectorProvider(InjectorFactory injectorFactory,
                          ConfigurableMetadataResolver metadataResolver) {
        this.injectorFactory = injectorFactory;
        this.metadataResolver = metadataResolver;
        parentProvider = null;
    }

    private GraphInjectorProvider(GraphInjectorProvider parentProvider, GraphContext context) {
        this.injectorFactory = parentProvider.injectorFactory;
        this.metadataResolver = parentProvider.metadataResolver;
        for (Map.Entry<Class<?>, Injector> entry : parentProvider.injectors.entrySet()) {
            injectors.put(entry.getKey(), entry.getValue().replicateWith(context));
        }
        for (Map.Entry<Class<?>, Instantiator> entry : parentProvider.instantiators.entrySet()) {
            instantiators.put(entry.getKey(), entry.getValue().replicateWith(context));
        }
        this.parentProvider = parentProvider;
    }

    Injector getInjector(Object injectionTarget,
                         InternalProvider internalProvider,
                         GraphLinker graphLinker,
                         GraphContext graphContext) {

        final Class<?> targetClass = injectionTarget.getClass();

        Injector injector = injectors.get(targetClass);

        if (injector == null) {

            final Qualifier qualifier = metadataResolver.resolve(targetClass, graphContext.errors());

            injector = injectorFactory.compositeInjector(
                    metadataResolver.resolveMetadata(
                            targetClass,
                            new ModuleMetadata(targetClass, qualifier, Module.NONE),
                            graphContext.errors()),
                    graphContext);

            graphLinker.linkAll(internalProvider, ResolutionContext.create());

            injectors.put(targetClass, injector);

            if (parentProvider != null) {
                parentProvider.injectors.put(targetClass, injector);
            }

        }

        return injector;
    }

    Instantiator getInstantiator(Class<?> injectionTarget,
                                 InternalProvider internalProvider,
                                 GraphLinker graphLinker,
                                 GraphContext graphContext) {

        Instantiator instantiator = instantiators.get(injectionTarget);

        if (instantiator == null) {

            final Qualifier qualifier = metadataResolver.resolve(injectionTarget, graphContext.errors());

            instantiator = injectorFactory.constructorInstantiator(
                    metadataResolver.resolveMetadata(
                            injectionTarget,
                            new ModuleMetadata(injectionTarget, qualifier, Module.NONE),
                            graphContext.errors()),
                    graphContext);

            graphLinker.linkAll(internalProvider, ResolutionContext.create());

            instantiators.put(injectionTarget, instantiator);

            if (parentProvider != null) {
                parentProvider.instantiators.put(injectionTarget, instantiator);
            }

        }

        return instantiator;
    }

    @Override public GraphInjectorProvider replicateWith(GraphContext context) {
        return new GraphInjectorProvider(this, context);
    }

}
