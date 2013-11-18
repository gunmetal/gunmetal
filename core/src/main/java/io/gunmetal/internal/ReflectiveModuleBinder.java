package io.gunmetal.internal;

import io.gunmetal.AccessLevel;
import io.gunmetal.AccessRestrictions;
import io.gunmetal.Component;
import io.gunmetal.CompositeQualifier;
import io.gunmetal.Module;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
class ReflectiveModuleBinder implements ModuleBinder {

    private final ComponentAdapterFactory componentAdapterFactory;
    private final MetadataAdapter metadataAdapter;

    ReflectiveModuleBinder(ComponentAdapterFactory componentAdapterFactory, MetadataAdapter metadataAdapter) {
        this.componentAdapterFactory = componentAdapterFactory;
        this.metadataAdapter = metadataAdapter;
    }

    @Override
    public void bind(final Class<?> module, InternalProvider provider, Binder binder) {

        final Module moduleAnnotation = module.getAnnotation(Module.class);

        if (moduleAnnotation == null) {
            throw new IllegalArgumentException("The module class [" + module.getName()
                    + "] must be annotated with @Module()");
        }

        final ModuleAdapter moduleAdapter = moduleAdapter(module, moduleAnnotation);

        bindComponentAnnotations(moduleAnnotation.components(), moduleAdapter, provider, binder);

        bindProviderMethods(module, moduleAdapter, provider, binder);

    }

    private ModuleAdapter moduleAdapter(final Class<?> module, final Module moduleAnnotation) {

        final AccessFilter<DependencyRequest> blackListFilter = blackListFilter(module, moduleAnnotation);

        final AccessFilter<DependencyRequest> whiteListFilter = whiteListFilter(module, moduleAnnotation);

        final AccessFilter<DependencyRequest> dependsOnFilter = dependsOnFilter(module);

        AccessLevel moduleAccessLevel = moduleAnnotation.access();

        final AccessFilter<Class<?>> moduleAccessFilter =
                AccessFilter.Factory.getAccessFilter(moduleAccessLevel, module);

        final CompositeQualifier compositeQualifier = Metadata.qualifier(module, metadataAdapter.qualifierAnnotation());

        return new ModuleAdapter() {

            @Override
            public Class<?> moduleClass() {
                return module;
            }

            @Override
            public CompositeQualifier compositeQualifier() {
                return compositeQualifier;
            }

            @Override
            public Class<?>[] referencedModules() {
                return moduleAnnotation.dependsOn();
            }

            @Override
            public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                // we use the single '&' because we want to process them all regardless if one fails
                // in order to collect all errors and report them back
                return moduleAccessFilter.isAccessibleTo(dependencyRequest.sourceModule().moduleClass())
                        & dependsOnFilter.isAccessibleTo(dependencyRequest)
                        & blackListFilter.isAccessibleTo(dependencyRequest)
                        & whiteListFilter.isAccessibleTo(dependencyRequest);
            }

        };
    }

    private AccessFilter<DependencyRequest> blackListFilter(final Class<?> module, Module moduleAnnotation) {

        Class<? extends AccessRestrictions.NotAccessibleFrom> blackListConfigClass =
                moduleAnnotation.notAccessibleFrom();

        if (blackListConfigClass == AccessRestrictions.NotAccessibleFrom.class) {

            return new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return true;
                }
            };

        }

        final Class[] blackListClasses;

        AccessRestrictions.Modules blackListModules =
                blackListConfigClass.getAnnotation(AccessRestrictions.Modules.class);

        if (blackListModules != null) {

            blackListClasses = blackListModules.value();

        } else {

            blackListClasses = new Class[]{};

        }

        final CompositeQualifier blackListQualifier = Metadata.qualifier(blackListConfigClass,
                metadataAdapter.qualifierAnnotation());

        return  new AccessFilter<DependencyRequest>() {

            @Override
            public boolean isAccessibleTo(DependencyRequest dependencyRequest) {

                Class<?> requestingSourceModuleClass = dependencyRequest.sourceModule().moduleClass();

                for (Class<?> blackListClass : blackListClasses) {

                    if (blackListClass == requestingSourceModuleClass) {

                        dependencyRequest.addError("The module [" + requestingSourceModuleClass.getName()
                                + "] does not have access to the module [" + module.getName() + "].");

                        return false;

                    }

                }

                boolean qualifierMatch = dependencyRequest.sourceQualifier().intersects(blackListQualifier);

                if (qualifierMatch) {
                    dependencyRequest.addError("The module [" + requestingSourceModuleClass.getName()
                            + "] does not have access to the module [" + module.getName() + "].");
                }

                return !qualifierMatch;

            }

        };

    }

    private AccessFilter<DependencyRequest> whiteListFilter(final Class<?> module, Module moduleAnnotation) {

        Class<? extends AccessRestrictions.OnlyAccessibleFrom> whiteListConfigClass =
                moduleAnnotation.onlyAccessibleFrom();

        if (whiteListConfigClass == AccessRestrictions.OnlyAccessibleFrom.class) {

            return new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return true;
                }
            };

        }

        final Class[] whiteListClasses;

        AccessRestrictions.Modules whiteListModules =
                whiteListConfigClass.getAnnotation(AccessRestrictions.Modules.class);

        if (whiteListModules != null) {

            whiteListClasses = whiteListModules.value();

        } else {

            whiteListClasses = new Class[]{};

        }

        final CompositeQualifier whiteListQualifier = Metadata.qualifier(whiteListConfigClass,
                metadataAdapter.qualifierAnnotation());

        return  new AccessFilter<DependencyRequest>() {

            @Override
            public boolean isAccessibleTo(DependencyRequest dependencyRequest) {

                Class<?> requestingSourceModuleClass = dependencyRequest.sourceModule().moduleClass();

                for (Class<?> whiteListClass : whiteListClasses) {
                    if (whiteListClass == requestingSourceModuleClass) {
                        return true;
                    }
                }

                boolean qualifierMatch = dependencyRequest.sourceQualifier().intersects(whiteListQualifier);

                if (!qualifierMatch) {

                    dependencyRequest.addError("The module [" + requestingSourceModuleClass.getName()
                            + "] does not have access to the module [" + module.getName() + "].");

                }

                return qualifierMatch;

            }

        };

    }

    private AccessFilter<DependencyRequest> dependsOnFilter(final Class<?> module) {

        return new AccessFilter<DependencyRequest>() {

            @Override
            public boolean isAccessibleTo(DependencyRequest dependencyRequest) {

                ModuleAdapter requestSourceModule = dependencyRequest.sourceModule();

                for (Class<?> dependency : requestSourceModule.referencedModules()) {
                    if (module == dependency) {
                        return true;
                    }
                }

                dependencyRequest.addError("The module [" + requestSourceModule.moduleClass().getName()
                        + "] does not have access to the module [" + module.getName() + "].");

                return false;

            }

        };

    }

    private void bindComponentAnnotations(Component[] components, final ModuleAdapter moduleAdapter,
                                          InternalProvider provider, Binder binder) {

        for (final Component component : components) {

            final CompositeQualifier compositeQualifier = Metadata.qualifier(
                    component.type(), moduleAdapter, metadataAdapter.qualifierAnnotation());

            ComponentMetadata componentMetadata = new ComponentMetadata() {
                @Override public AnnotatedElement provider() {
                    return component.type();
                }
                @Override public Class<?> providerClass() {
                    return component.type();
                }
                @Override public ModuleAdapter moduleAdapter() {
                    return moduleAdapter;
                }
                @Override public CompositeQualifier compositeQualifier() {
                    return compositeQualifier;
                }
            };

            final AccessFilter<Class<?>> accessFilter =
                    AccessFilter.Factory.getAccessFilter(component.access(), component.type());

            ComponentAdapter<?> componentAdapter =
                    componentAdapterFactory.create(componentMetadata, new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return moduleAdapter.isAccessibleTo(dependencyRequest)
                            && accessFilter.isAccessibleTo(dependencyRequest.sourceOrigin());
                }
            }, provider);

            for (Class<?> target : component.targets()) {

                binder.bind(
                        Metadata.dependency(target, componentAdapter.metadata().compositeQualifier()),
                        componentAdapter);

            }

        }
    }

    private void bindProviderMethods(Class<?> module, final ModuleAdapter moduleAdapter,
                                     InternalProvider provider, Binder binder) {

        for (final Method method : module.getDeclaredMethods()) {

            int modifiers = method.getModifiers();

            if (!Modifier.isStatic(modifiers)) {
                throw new IllegalArgumentException("A module's provider methods must be static.  The method ["
                        + method.getName() + "] in module [" + module.getName() + "] is not static.");
            }

            if (method.getReturnType().equals(Void.TYPE)) {
                throw new IllegalArgumentException("A module's provider methods must have a return type.  The method ["
                        + method.getName() + "] in module [" + module.getName() + "] has a void return type.");
            }

            final CompositeQualifier compositeQualifier = Metadata.qualifier(
                    method, moduleAdapter, metadataAdapter.qualifierAnnotation());

            ComponentMetadata componentMetadata = new ComponentMetadata() {
                @Override public AnnotatedElement provider() {
                    return method;
                }
                @Override public Class<?> providerClass() {
                    return method.getDeclaringClass();
                }
                @Override public ModuleAdapter moduleAdapter() {
                    return moduleAdapter;
                }
                @Override public CompositeQualifier compositeQualifier() {
                    return compositeQualifier;
                }
            };

            final AccessFilter<Class<?>> accessFilter = AccessFilter.Factory.getAccessFilter(method);

            ComponentAdapter<?> componentAdapter =
                    componentAdapterFactory.create(componentMetadata, new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return moduleAdapter.isAccessibleTo(dependencyRequest)
                            && accessFilter.isAccessibleTo(dependencyRequest.sourceOrigin());
                }
            }, provider);

            // TODO targeted return type check, better type ref impl

            final Type target = method.getGenericReturnType();

            binder.bind(
                    Metadata.dependency(target, componentAdapter.metadata().compositeQualifier()),
                    componentAdapter);

        }

    }

}
