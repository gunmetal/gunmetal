package io.gunmetal.internal;

import io.gunmetal.Module;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.Qualifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rees.byars
 */
final class ComponentInjectors implements Replicable<ComponentInjectors> {

    private final Map<Class<?>, Injector> injectors = new ConcurrentHashMap<>(1, .75f, 4);
    private final InjectorFactory injectorFactory;
    private final ConfigurableMetadataResolver metadataResolver;
    private final ComponentInjectors parentInjectors;

    ComponentInjectors(InjectorFactory injectorFactory,
                       ConfigurableMetadataResolver metadataResolver) {
        this.injectorFactory = injectorFactory;
        this.metadataResolver = metadataResolver;
        parentInjectors = null;
    }

    private ComponentInjectors(ComponentInjectors parentInjectors, ComponentContext context) {
        this.injectorFactory = parentInjectors.injectorFactory;
        this.metadataResolver = parentInjectors.metadataResolver;
        for (Map.Entry<Class<?>, Injector> entry : parentInjectors.injectors.entrySet()) {
            injectors.put(entry.getKey(), entry.getValue().replicateWith(context));
        }
        this.parentInjectors = parentInjectors;
    }

    Injector getInjector(Object injectionTarget,
                         DependencySupplier dependencySupplier,
                         ComponentLinker componentLinker,
                         ComponentContext componentContext) {

        final Class<?> targetClass = injectionTarget.getClass();

        Injector injector = injectors.get(targetClass);

        if (injector == null) {

            final Qualifier qualifier = metadataResolver.resolve(targetClass);

            injector = injectorFactory.compositeInjector(
                    targetClass,
                    metadataResolver.resolveMetadata(
                            targetClass,
                            new ModuleMetadata(targetClass, qualifier, Module.NONE),
                            componentContext.errors()),
                    componentContext);

            componentLinker.linkAll(dependencySupplier, componentContext.newResolutionContext());

            injectors.put(targetClass, injector);

            if (parentInjectors != null) {
                parentInjectors.injectors.put(targetClass, injector);
            }

        }

        return injector;
    }

    @Override public ComponentInjectors replicateWith(ComponentContext context) {
        return new ComponentInjectors(this, context);
    }

}
