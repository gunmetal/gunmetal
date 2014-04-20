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
import io.gunmetal.Module;
import io.gunmetal.WhiteList;
import io.gunmetal.spi.AnnotationResolver;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.ComponentMetadataResolver;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.TypeKey;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
class HandlerFactoryImpl implements HandlerFactory {

    private final ComponentAdapterFactory componentAdapterFactory;
    private final AnnotationResolver<Qualifier> qualifierResolver;
    private final ComponentMetadataResolver componentMetadataResolver;

    HandlerFactoryImpl(ComponentAdapterFactory componentAdapterFactory,
                       AnnotationResolver<Qualifier> qualifierResolver,
                       ComponentMetadataResolver componentMetadataResolver) {
        this.componentAdapterFactory = componentAdapterFactory;
        this.qualifierResolver = qualifierResolver;
        this.componentMetadataResolver = componentMetadataResolver;
    }

    @Override public List<DependencyRequestHandler<?>> createHandlersForModule(final Class<?> module) {
        final Module moduleAnnotation = module.getAnnotation(Module.class);
        if (moduleAnnotation == null) {
            throw new IllegalArgumentException("The module class [" + module.getName()
                    + "] must be annotated with @Module()");
        }
        RequestVisitor moduleRequestVisitor = moduleRequestVisitor(module, moduleAnnotation);
        ModuleMetadata moduleMetadata = moduleMetadata(module, moduleAnnotation);
        List<DependencyRequestHandler<?>> requestHandlers = new LinkedList<>();
        addForProviderMethods(
                module, requestHandlers, moduleRequestVisitor, moduleMetadata);
        return requestHandlers;

    }

    @Override public <T> DependencyRequestHandler<T> attemptToCreateHandlerFor(
            final DependencyRequest<T> dependencyRequest) {
        final Dependency<T> dependency = dependencyRequest.dependency();
        final TypeKey<T> typeKey = dependency.typeKey();
        if (!isInstantiable(typeKey.raw())) {
            return null;
        }
        final Class<? super T> cls = typeKey.raw();
        final ModuleMetadata moduleMetadata = new ModuleMetadata(cls, dependency.qualifier(), new Class<?>[0]);
        ComponentAdapter<T> componentAdapter = componentAdapterFactory.withClassProvider(
                componentMetadataResolver.resolveMetadata(cls, moduleMetadata));
        return requestHandler(
                componentAdapter,
                Collections.<Dependency<? super T>>singletonList(dependency),
                RequestVisitor.NONE,
                AccessFilter.Factory.getAccessFilter(typeKey.raw()));
    }

    private RequestVisitor moduleRequestVisitor(final Class<?> module, final Module moduleAnnotation) {
        final RequestVisitor blackListVisitor = blackListVisitor(module, moduleAnnotation);
        final RequestVisitor whiteListVisitor = whiteListVisitor(module, moduleAnnotation);
        final RequestVisitor dependsOnVisitor = dependsOnVisitor(module);
        final RequestVisitor moduleClassVisitor = new RequestVisitor() {
            AccessFilter<Class<?>> classAccessFilter =
                    AccessFilter.Factory.getAccessFilter(moduleAnnotation.access(), module);
            @Override public void visit(
                    DependencyRequest<?> dependencyRequest, MutableDependencyResponse<?> response) {
                if (!classAccessFilter.isAccessibleTo(dependencyRequest.sourceModule().moduleClass())) {
                    response.addError(
                            "The module [" + dependencyRequest.sourceModule().moduleClass().getName()
                                    + "] does not have access to [" + classAccessFilter.filteredElement() + "]"
                    );
                }
            }
        };

        return new RequestVisitor() {
            @Override public void visit(
                    DependencyRequest<?> dependencyRequest, MutableDependencyResponse<?> response) {
                moduleClassVisitor.visit(dependencyRequest, response);
                dependsOnVisitor.visit(dependencyRequest, response);
                blackListVisitor.visit(dependencyRequest, response);
                whiteListVisitor.visit(dependencyRequest, response);
            }
        };
    }

    private ModuleMetadata moduleMetadata(final Class<?> module, final Module moduleAnnotation) {
        final Qualifier qualifier = qualifierResolver.resolve(module);
        return new ModuleMetadata(module, qualifier, moduleAnnotation.dependsOn());
    }

    private RequestVisitor blackListVisitor(final Class<?> module, Module moduleAnnotation) {

        Class<? extends BlackList> blackListConfigClass =
                moduleAnnotation.notAccessibleFrom();

        if (blackListConfigClass == BlackList.class) {
            return RequestVisitor.NONE;
        }

        final Class[] blackListClasses;

        BlackList.Modules blackListModules =
                blackListConfigClass.getAnnotation(BlackList.Modules.class);

        if (blackListModules != null) {

            blackListClasses = blackListModules.value();

        } else {

            blackListClasses = new Class<?>[]{};

        }

        final Qualifier blackListQualifier = qualifierResolver.resolve(blackListConfigClass);

        return new RequestVisitor() {

            @Override public void visit(
                    DependencyRequest<?> dependencyRequest, MutableDependencyResponse<?> response) {

                Class<?> requestingSourceModuleClass = dependencyRequest.sourceModule().moduleClass();

                for (Class<?> blackListClass : blackListClasses) {

                    if (blackListClass == requestingSourceModuleClass) {

                        response.addError("The module [" + requestingSourceModuleClass.getName()
                                + "] does not have access to the module [" + module.getName() + "].");

                    }

                }

                boolean qualifierMatch =
                        blackListQualifier.qualifiers().length > 0
                        && dependencyRequest.sourceQualifier().intersects(blackListQualifier);

                if (qualifierMatch) {
                    response.addError("The module [" + requestingSourceModuleClass.getName()
                            + "] does not have access to the module [" + module.getName() + "].");
                }

            }

        };

    }

    private RequestVisitor whiteListVisitor(final Class<?> module, Module moduleAnnotation) {

        Class<? extends WhiteList> whiteListConfigClass =
                moduleAnnotation.onlyAccessibleFrom();

        if (whiteListConfigClass == WhiteList.class) {
            return RequestVisitor.NONE;
        }

        final Class[] whiteListClasses;

        WhiteList.Modules whiteListModules =
                whiteListConfigClass.getAnnotation(WhiteList.Modules.class);

        if (whiteListModules != null) {

            whiteListClasses = whiteListModules.value();

        } else {

            whiteListClasses = new Class<?>[]{};

        }

        final Qualifier whiteListQualifier = qualifierResolver.resolve(whiteListConfigClass);

        return new RequestVisitor() {

            @Override public void visit(
                    DependencyRequest<?> dependencyRequest, MutableDependencyResponse<?> response) {

                Class<?> requestingSourceModuleClass = dependencyRequest.sourceModule().moduleClass();

                for (Class<?> whiteListClass : whiteListClasses) {
                    if (whiteListClass == requestingSourceModuleClass) {
                        return;
                    }
                }

                boolean qualifierMatch = dependencyRequest.sourceQualifier().intersects(whiteListQualifier);

                if (!qualifierMatch) {

                    response.addError("The module [" + requestingSourceModuleClass.getName()
                            + "] does not have access to the module [" + module.getName() + "].");

                }

            }

        };

    }

    private RequestVisitor dependsOnVisitor(final Class<?> module) {

        return new RequestVisitor() {

            @Override public void visit(
                    DependencyRequest<?> dependencyRequest, MutableDependencyResponse<?> response) {

                ModuleMetadata requestSourceModule = dependencyRequest.sourceModule();

                if (module == requestSourceModule.moduleClass()) {
                    return;
                }

                if (requestSourceModule.referencedModules().length == 0) {
                    return; //TODO
                }

                for (Class<?> dependency : requestSourceModule.referencedModules()) {
                    if (module == dependency) {
                        return;
                    }
                }

                response.addError("The module [" + requestSourceModule.moduleClass().getName()
                        + "] does not have access to the module [" + module.getName() + "].");

            }

        };

    }

    private void addForProviderMethods(
            Class<?> module,
            List<DependencyRequestHandler<?>> requestHandlers,
            final RequestVisitor moduleRequestVisitor,
            final ModuleMetadata moduleMetadata) {

        for (final Method method : module.getDeclaredMethods()) {

            requestHandlers.add(requestHandler(
                    method,
                    module,
                    moduleRequestVisitor,
                    moduleMetadata));
        }

    }

    private <T> DependencyRequestHandler<T> requestHandler(
            final Method method,
            Class<?> module,
            final RequestVisitor moduleRequestVisitor,
            final ModuleMetadata moduleMetadata) {

        int modifiers = method.getModifiers();

        if (!Modifier.isStatic(modifiers)) {
            throw new IllegalArgumentException("A module's provider methods must be static.  The method ["
                    + method.getName() + "] in module [" + module.getName() + "] is not static.");
        }

        ComponentMetadata<Method> componentMetadata = componentMetadataResolver.resolveMetadata(method, moduleMetadata);

        // TODO targeted return type check
        final List<Dependency<? super T>> dependencies = Collections.<Dependency<? super T>>singletonList(
                Dependency.from(componentMetadata.qualifier(), method.getGenericReturnType()));

        return requestHandler(
                componentAdapterFactory.<T>withMethodProvider(componentMetadata),
                dependencies,
                moduleRequestVisitor,
                AccessFilter.Factory.getAccessFilter(method));
    }

    private <T> DependencyRequestHandler<T> requestHandler(
                                                     final ComponentAdapter<T> componentAdapter,
                                                     final List<Dependency<? super T>> targets,
                                                     final RequestVisitor moduleRequestVisitor,
                                                     final AccessFilter<Class<?>> classAccessFilter) {

        return new DependencyRequestHandler<T>() {

            @Override public List<Dependency<? super T>> targets() {
                return targets;
            }

            @Override public List<Dependency<?>> dependencies() {
                return componentAdapter.dependencies();
            }

            @Override public DependencyResponse<T> handle(DependencyRequest<? super T> dependencyRequest) {
                MutableDependencyResponse<T> response =
                        new DependencyResponseImpl<>(componentAdapter.provisionStrategy());
                moduleRequestVisitor.visit(dependencyRequest, response);
                if (!classAccessFilter.isAccessibleTo(dependencyRequest.sourceOrigin())) {
                    response.addError(
                            "The class [" + dependencyRequest.sourceOrigin().getName()
                            + "] does not have access to [" + classAccessFilter.filteredElement() + "]"
                    );
                }
                return response;
            }

            @Override public ProvisionStrategy<T> force() {
                return componentAdapter.provisionStrategy();
            }

            @Override public ComponentMetadata<?> componentMetadata() {
                return componentAdapter.metadata();
            }

        };
    }

    private boolean isInstantiable(Class<?> cls) {
        if (cls.isInterface()
                || cls.isArray()
                || cls.isEnum()
                || cls.isPrimitive()
                || cls.isSynthetic()
                || cls.isAnnotation()) {
            return false;
        }
        int modifiers = cls.getModifiers();
        return !Modifier.isAbstract(modifiers);
    }

    private interface MutableDependencyResponse<T> extends DependencyResponse<T> {
        void addError(String errorMessage);
    }

    private interface RequestVisitor {

        RequestVisitor NONE = new RequestVisitor() {
            @Override public void visit(
                    DependencyRequest<?> dependencyRequest, MutableDependencyResponse<?> dependencyResponse) { }
        };

        void visit(DependencyRequest<?> dependencyRequest, MutableDependencyResponse<?> dependencyResponse);
    }

    private static class DependencyResponseImpl<T> implements MutableDependencyResponse<T> {

        List<String> errors;
        final ProvisionStrategy<? extends T> provisionStrategy;

        DependencyResponseImpl(ProvisionStrategy<T> provisionStrategy) {
            this.provisionStrategy = provisionStrategy;
        }

        @Override public void addError(String errorMessage) {
            if (errors == null) {
                errors = new LinkedList<>();
            }
            errors.add(errorMessage);
        }

        @Override public ValidatedDependencyResponse<T> validateResponse() {
            if (errors != null) {
                StringBuilder stringBuilder = new StringBuilder("There were errors processing a dependency \n");
                for (String error : errors) {
                    stringBuilder.append("    ").append(error).append("\n");
                }
                throw new DependencyException(stringBuilder.toString());
            }
            return new ValidatedDependencyResponse<T>() {
                @Override public ProvisionStrategy<? extends T> getProvisionStrategy() {
                    return provisionStrategy;
                }
                @Override public ValidatedDependencyResponse<T> validateResponse() {
                    return this;
                }
            };
        }
    }

}
