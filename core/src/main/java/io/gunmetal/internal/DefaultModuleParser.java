package io.gunmetal.internal;

import io.gunmetal.AccessLevel;
import io.gunmetal.BlackList;
import io.gunmetal.Component;
import io.gunmetal.CompositeQualifier;
import io.gunmetal.Module;
import io.gunmetal.WhiteList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * @author rees.byars
 */
class DefaultModuleParser implements ModuleParser {

    private final ProvisionStrategyFactory provisionStrategyFactory;
    private final AnnotationResolver<CompositeQualifier> qualifierResolver;

    DefaultModuleParser(ProvisionStrategyFactory provisionStrategyFactory,
                        AnnotationResolver<CompositeQualifier> qualifierResolver) {
        this.provisionStrategyFactory = provisionStrategyFactory;
        this.qualifierResolver = qualifierResolver;
    }

    @Override
    public List<ComponentAdapter<?>> parse(final Class<?> module, InternalProvider provider) {

        final Module moduleAnnotation = module.getAnnotation(Module.class);

        if (moduleAnnotation == null) {
            throw new IllegalArgumentException("The module class [" + module.getName()
                    + "] must be annotated with @Module()");
        }

        final ModuleAdapter moduleAdapter = moduleAdapter(module, moduleAnnotation);

        bindComponentAnnotations(moduleAnnotation.components(), moduleAdapter, provider);

        bindProviderMethods(module, moduleAdapter, provider);

        return null; // TODO

    }

    private ModuleAdapter moduleAdapter(final Class<?> module, final Module moduleAnnotation) {

        final AccessFilter<DependencyRequest> blackListFilter = blackListFilter(module, moduleAnnotation);

        final AccessFilter<DependencyRequest> whiteListFilter = whiteListFilter(module, moduleAnnotation);

        final AccessFilter<DependencyRequest> dependsOnFilter = dependsOnFilter(module);

        AccessLevel moduleAccessLevel = moduleAnnotation.access();

        final AccessFilter<Class<?>> moduleAccessFilter =
                AccessFilter.Factory.getAccessFilter(moduleAccessLevel, module);

        final CompositeQualifier compositeQualifier = qualifierResolver.resolve(module);

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

        Class<? extends BlackList> blackListConfigClass =
                moduleAnnotation.notAccessibleFrom();

        if (blackListConfigClass == BlackList.class) {
            return new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return true;
                }
            };
        }

        final Class[] blackListClasses;

        BlackList.Modules blackListModules =
                blackListConfigClass.getAnnotation(BlackList.Modules.class);

        if (blackListModules != null) {

            blackListClasses = blackListModules.value();

        } else {

            blackListClasses = new Class[]{};

        }

        final CompositeQualifier blackListQualifier = qualifierResolver.resolve(blackListConfigClass);

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

        Class<? extends WhiteList> whiteListConfigClass =
                moduleAnnotation.onlyAccessibleFrom();

        if (whiteListConfigClass == WhiteList.class) {
            return new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return true;
                }
            };
        }

        final Class[] whiteListClasses;

        WhiteList.Modules whiteListModules =
                whiteListConfigClass.getAnnotation(WhiteList.Modules.class);

        if (whiteListModules != null) {

            whiteListClasses = whiteListModules.value();

        } else {

            whiteListClasses = new Class[]{};

        }

        final CompositeQualifier whiteListQualifier = qualifierResolver.resolve(whiteListConfigClass);

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

    private void bindComponentAnnotations(Component[] components,
                                          final ModuleAdapter moduleAdapter,
                                          InternalProvider provider) {

        for (final Component component : components) {

            final CompositeQualifier compositeQualifier = qualifierResolver.resolve(
                    component.type(), moduleAdapter.compositeQualifier());

            ComponentMetadata<Class> componentMetadata = new ComponentMetadata<Class>() {
                @Override public Class<?> provider() {
                    return component.type();
                }
                @Override public Class<?> providerClass() {
                    return component.type();
                }
                @Override public ModuleAdapter moduleAdapter() {
                    return moduleAdapter;
                }
                @Override public CompositeQualifier qualifier() {
                    return compositeQualifier;
                }
                @Override public Collection<Type> targets() {
                    return null;  //TODO
                }
            };

            final AccessFilter<Class<?>> accessFilter =
                    AccessFilter.Factory.getAccessFilter(component.access(), component.type());

            ProvisionStrategy<?> provisionStrategy =
                    provisionStrategyFactory.create(componentMetadata, new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return moduleAdapter.isAccessibleTo(dependencyRequest)
                            && accessFilter.isAccessibleTo(dependencyRequest.sourceOrigin());
                }
            }, provider);

            for (Class<?> target : component.targets()) {

                //TODO add to collection and return

            }

        }
    }

    private void bindProviderMethods(Class<?> module,
                                     final ModuleAdapter moduleAdapter,
                                     InternalProvider provider) {

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

            final CompositeQualifier compositeQualifier =
                    qualifierResolver.resolve(method, moduleAdapter.compositeQualifier());

            ComponentMetadata<Method> componentMetadata = new ComponentMetadata<Method>() {
                @Override public Method provider() {
                    return method;
                }
                @Override public Class<?> providerClass() {
                    return method.getDeclaringClass();
                }
                @Override public ModuleAdapter moduleAdapter() {
                    return moduleAdapter;
                }
                @Override public CompositeQualifier qualifier() {
                    return compositeQualifier;
                }
                @Override public Collection<Type> targets() {
                    return null;  //TODO
                }
            };

            final AccessFilter<Class<?>> accessFilter = AccessFilter.Factory.getAccessFilter(method);

            ProvisionStrategy<?> provisionStrategy =
                    provisionStrategyFactory.create(componentMetadata, new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return moduleAdapter.isAccessibleTo(dependencyRequest)
                            && accessFilter.isAccessibleTo(dependencyRequest.sourceOrigin());
                }
            }, provider);

            // TODO targeted return type check, better type ref impl

            final Type target = method.getGenericReturnType();

            // TODO

        }

    }

}
