/*
 * Copyright (c) 2013.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gunmetal.internal;

import io.gunmetal.BlackList;
import io.gunmetal.Component;
import io.gunmetal.Module;
import io.gunmetal.WhiteList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
class ModuleParserImpl implements ModuleParser {

    private final ProvisionStrategyFactory provisionStrategyFactory;
    private final AnnotationResolver<Qualifier> qualifierResolver;

    ModuleParserImpl(ProvisionStrategyFactory provisionStrategyFactory,
                     AnnotationResolver<Qualifier> qualifierResolver) {
        this.provisionStrategyFactory = provisionStrategyFactory;
        this.qualifierResolver = qualifierResolver;
    }

    @Override
    public List<ComponentAdapter<?>> parse(final Class<?> module) {

        final Module moduleAnnotation = module.getAnnotation(Module.class);

        if (moduleAnnotation == null) {
            throw new IllegalArgumentException("The module class [" + module.getName()
                    + "] must be annotated with @Module()");
        }

        final ModuleAdapter moduleAdapter = moduleAdapter(module, moduleAnnotation);

        List<ComponentAdapter<?>> componentAdapters = new LinkedList<ComponentAdapter<?>>();

        addForComponentAnnotations(moduleAnnotation.components(), componentAdapters, moduleAdapter);

        addForProviderMethods(module, componentAdapters, moduleAdapter);

        return componentAdapters;

    }

    private ModuleAdapter moduleAdapter(final Class<?> module, final Module moduleAnnotation) {

        final AccessFilter<DependencyRequest> blackListFilter = blackListFilter(module, moduleAnnotation);
        final AccessFilter<DependencyRequest> whiteListFilter = whiteListFilter(module, moduleAnnotation);
        final AccessFilter<DependencyRequest> dependsOnFilter = dependsOnFilter(module);
        final AccessFilter<Class<?>> moduleAccessFilter =
                AccessFilter.Factory.getAccessFilter(moduleAnnotation.access(), module);
        final Qualifier qualifier = qualifierResolver.resolve(module);

        return new ModuleAdapter() {

            @Override public Class<?> moduleClass() {
                return module;
            }

            @Override public Qualifier qualifier() {
                return qualifier;
            }

            @Override public Class<?>[] referencedModules() {
                return moduleAnnotation.dependsOn();
            }

            @Override public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
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

        final Qualifier blackListQualifier = qualifierResolver.resolve(blackListConfigClass);

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

        final Qualifier whiteListQualifier = qualifierResolver.resolve(whiteListConfigClass);

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

    private void addForComponentAnnotations(
            Component[] components,
            List<ComponentAdapter<?>> componentAdapters,
            final ModuleAdapter moduleAdapter) {

        for (final Component component : components) {

            final Qualifier qualifier = qualifierResolver.resolve(
                    component.type(), moduleAdapter.qualifier());

            final Collection<TypeKey<?>> typeKeys;
            Class<?>[] targets = component.targets();

            if (targets.length == 0) {
                typeKeys = Collections.<TypeKey<?>>singletonList(Types.typeKey(component.type()));
            } else {
                typeKeys = Types.typeKeys(targets);
            }

            final ComponentMetadata<Class> componentMetadata = new ComponentMetadata<Class>() {
                @Override public Class<?> provider() {
                    return component.type();
                }
                @Override public Class<?> providerClass() {
                    return component.type();
                }
                @Override public ModuleAdapter moduleAdapter() {
                    return moduleAdapter;
                }
                @Override public Qualifier qualifier() {
                    return qualifier;
                }
                @Override public Collection<TypeKey<?>> targets() {
                    return typeKeys;
                }
            };

            final AccessFilter<Class<?>> accessFilter =
                    AccessFilter.Factory.getAccessFilter(component.access(), component.type());

            final ProvisionStrategy<?> provisionStrategy =
                    provisionStrategyFactory.withClassProvider(componentMetadata);

            componentAdapters.add(componentAdapter(componentMetadata, new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return moduleAdapter.isAccessibleTo(dependencyRequest)
                            && accessFilter.isAccessibleTo(dependencyRequest.sourceOrigin());
                }
            }, provisionStrategy));

        }
    }

    private void addForProviderMethods(
            Class<?> module,
            List<ComponentAdapter<?>> componentAdapters,
            final ModuleAdapter moduleAdapter) {

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

            final Qualifier qualifier =
                    qualifierResolver.resolve(method, moduleAdapter.qualifier());

            // TODO targeted return type check, better type ref impl
            final Collection<TypeKey<?>> typeKeys =
                    Collections.<TypeKey<?>>singletonList(Types.typeKey(method.getGenericReturnType()));

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
                @Override public Qualifier qualifier() {
                    return qualifier;
                }
                @Override public Collection<TypeKey<?>> targets() {
                    return typeKeys;
                }
            };

            final AccessFilter<Class<?>> accessFilter = AccessFilter.Factory.getAccessFilter(method);

            ProvisionStrategy<?> provisionStrategy =
                    provisionStrategyFactory.withMethodProvider(componentMetadata);

            componentAdapters.add(componentAdapter(componentMetadata, new AccessFilter<DependencyRequest>() {
                @Override
                public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                    return moduleAdapter.isAccessibleTo(dependencyRequest)
                            && accessFilter.isAccessibleTo(dependencyRequest.sourceOrigin());
                }
            }, provisionStrategy));

        }

    }

    private <T> ComponentAdapter<T> componentAdapter(
            final ComponentMetadata<?> componentMetadata,
            final AccessFilter<DependencyRequest> accessFilter,
            final ProvisionStrategy<T> provisionStrategy) {
        return new ComponentAdapter<T>() {
            @Override public ComponentMetadata metadata() {
                return componentMetadata;
            }
            @Override public AccessFilter<DependencyRequest> filter() {
                return accessFilter;
            }
            @Override public ProvisionStrategy<T> provisionStrategy() {
                return provisionStrategy;
            }
        };
    }

}
