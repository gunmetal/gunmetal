package io.gunmetal.internal;

import io.gunmetal.Module;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.QualifierResolver;
import io.gunmetal.spi.ResourceMetadataResolver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rees.byars
 */
final class ComponentInjectors implements Replicable<ComponentInjectors> {

    private final Map<Class<?>, Injector> injectors = new ConcurrentHashMap<>(1, .75f, 4);
    private final InjectorFactory injectorFactory;
    private final QualifierResolver qualifierResolver;
    private final ResourceMetadataResolver resourceMetadataResolver;
    private final ComponentInjectors parentInjectors;

    ComponentInjectors(InjectorFactory injectorFactory,
                       QualifierResolver qualifierResolver,
                       ResourceMetadataResolver resourceMetadataResolver) {
        this.injectorFactory = injectorFactory;
        this.qualifierResolver = qualifierResolver;
        this.resourceMetadataResolver = resourceMetadataResolver;
        parentInjectors = null;
    }

    private ComponentInjectors(ComponentInjectors parentInjectors, ComponentContext context) {
        this.injectorFactory = parentInjectors.injectorFactory;
        this.qualifierResolver = parentInjectors.qualifierResolver;
        this.resourceMetadataResolver = parentInjectors.resourceMetadataResolver;
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

            final Qualifier qualifier = qualifierResolver.resolve(targetClass);

            injector = injectorFactory.compositeInjector(
                    targetClass,
                    resourceMetadataResolver.resolveMetadata(
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
