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
import io.gunmetal.Library;
import io.gunmetal.Module;
import io.gunmetal.WhiteList;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.ComponentMetadataResolver;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.QualifierResolver;
import io.gunmetal.spi.TypeKey;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
class HandlerFactoryImpl implements HandlerFactory {

    private final ComponentAdapterFactory componentAdapterFactory;
    private final QualifierResolver qualifierResolver;
    private final ComponentMetadataResolver componentMetadataResolver;

    HandlerFactoryImpl(ComponentAdapterFactory componentAdapterFactory,
                       QualifierResolver qualifierResolver,
                       ComponentMetadataResolver componentMetadataResolver) {
        this.componentAdapterFactory = componentAdapterFactory;
        this.qualifierResolver = qualifierResolver;
        this.componentMetadataResolver = componentMetadataResolver;
    }

    @Override public List<DependencyRequestHandler<?>> createHandlersForModule(final Class<?> module,
                                                                               GraphContext context) {
        if (context.loadedModules().contains(module)) {
            return Collections.emptyList();
        }

        final Module moduleAnnotation = module.getAnnotation(Module.class);
        if (moduleAnnotation == null) {
            throw new IllegalArgumentException("The module class [" + module.getName()
                    + "] must be annotated with @Module()");
        }
        final RequestVisitor moduleRequestVisitor = moduleRequestVisitor(module, moduleAnnotation);
        final ModuleMetadata moduleMetadata = moduleMetadata(module, moduleAnnotation);
        final List<DependencyRequestHandler<?>> requestHandlers = new LinkedList<>();
        if (moduleAnnotation.stateful()) {
            Arrays.stream(module.getDeclaredMethods()).forEach(m -> requestHandlers
                    .add(statefulRequestHandler(m, module, moduleRequestVisitor, moduleMetadata, context)));
        } else {
            Arrays.stream(module.getDeclaredMethods()).forEach(m -> requestHandlers
                    .add(requestHandler(m, module, moduleRequestVisitor, moduleMetadata, context)));
        }
        for (Class<?> library : moduleAnnotation.subsumes()) {
            if (library.isAnnotationPresent(Module.class)) {
                // TODO better message
                throw new IllegalArgumentException("A class with @Module cannot be subsumed");
            }
            if (!library.isAnnotationPresent(Library.class)) {
                // TODO better message
                throw new IllegalArgumentException("A class without @Library cannot be subsumed");
            }
            Arrays.stream(library.getDeclaredMethods()).forEach(m -> requestHandlers
                    .add(requestHandler(m, module, moduleRequestVisitor, moduleMetadata, context)));
        }
        for (Class<?> m : moduleAnnotation.dependsOn()) {
            requestHandlers.addAll(createHandlersForModule(m, context));
        }
        return requestHandlers;

    }

    @Override public <T> DependencyRequestHandler<T> attemptToCreateHandlerFor(
            final DependencyRequest<T> dependencyRequest, GraphContext context) {
        final Dependency<T> dependency = dependencyRequest.dependency();
        final TypeKey<T> typeKey = dependency.typeKey();
        if (!isInstantiable(typeKey.raw())) {
            return null;
        }
        final Class<? super T> cls = typeKey.raw();
        final ModuleMetadata moduleMetadata = new ModuleMetadata(cls, dependency.qualifier(), new Class<?>[0]);
        ComponentAdapter<T> componentAdapter = componentAdapterFactory.withClassProvider(
                componentMetadataResolver.resolveMetadata(cls, moduleMetadata), context);
        return requestHandler(
                componentAdapter,
                Collections.<Dependency<? super T>>singletonList(dependency),
                RequestVisitor.NONE,
                AccessFilter.create(typeKey.raw()));
    }

    private RequestVisitor moduleRequestVisitor(final Class<?> module, final Module moduleAnnotation) {
        final RequestVisitor blackListVisitor = blackListVisitor(module, moduleAnnotation);
        final RequestVisitor whiteListVisitor = whiteListVisitor(module, moduleAnnotation);
        final RequestVisitor dependsOnVisitor = dependsOnVisitor(module);
        final RequestVisitor moduleClassVisitor = new RequestVisitor() {
            AccessFilter<Class<?>> classAccessFilter =
                    AccessFilter.create(moduleAnnotation.access(), module);
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

        return (dependencyRequest, response) -> {
            moduleClassVisitor.visit(dependencyRequest, response);
            dependsOnVisitor.visit(dependencyRequest, response);
            blackListVisitor.visit(dependencyRequest, response);
            whiteListVisitor.visit(dependencyRequest, response);
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

        final Class<?>[] blackListClasses;

        BlackList.Modules blackListModules =
                blackListConfigClass.getAnnotation(BlackList.Modules.class);

        if (blackListModules != null) {

            blackListClasses = blackListModules.value();

        } else {

            blackListClasses = new Class<?>[]{};

        }

        final Qualifier blackListQualifier = qualifierResolver.resolve(blackListConfigClass);

        return (dependencyRequest, response) -> {

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

        };

    }

    private RequestVisitor whiteListVisitor(final Class<?> module, Module moduleAnnotation) {

        Class<? extends WhiteList> whiteListConfigClass =
                moduleAnnotation.onlyAccessibleFrom();

        if (whiteListConfigClass == WhiteList.class) {
            return RequestVisitor.NONE;
        }

        final Class<?>[] whiteListClasses;

        WhiteList.Modules whiteListModules =
                whiteListConfigClass.getAnnotation(WhiteList.Modules.class);

        if (whiteListModules != null) {

            whiteListClasses = whiteListModules.value();

        } else {

            whiteListClasses = new Class<?>[]{};

        }

        final Qualifier whiteListQualifier = qualifierResolver.resolve(whiteListConfigClass);

        return (dependencyRequest, response) -> {

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

        };

    }

    private RequestVisitor dependsOnVisitor(final Class<?> module) {

        return (dependencyRequest, response) -> {

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

        };

    }

    private <T> DependencyRequestHandler<T> requestHandler(
            final Method method,
            Class<?> module,
            final RequestVisitor moduleRequestVisitor,
            final ModuleMetadata moduleMetadata,
            GraphContext context) {

        int modifiers = method.getModifiers();

        if (!Modifier.isStatic(modifiers)) {
            throw new IllegalArgumentException("A module's provider methods must be static.  The method ["
                    + method.getName() + "] in module [" + module.getName() + "] is not static.");
        }

        if (method.getReturnType() == void.class) {
            throw new IllegalArgumentException("A module's provider methods cannot have a void return type.  The method ["
                    + method.getName() + "] in module [" + module.getName() + "] is returns void.");
        }

        ComponentMetadata<Method> componentMetadata = componentMetadataResolver.resolveMetadata(method, moduleMetadata);

        // TODO targeted return type check
        final List<Dependency<? super T>> dependencies = Collections.<Dependency<? super T>>singletonList(
                Dependency.from(componentMetadata.qualifier(), method.getGenericReturnType()));

        return requestHandler(
                componentAdapterFactory.<T>withMethodProvider(componentMetadata, context),
                dependencies,
                moduleRequestVisitor,
                AccessFilter.create(method));
    }

    private <T> DependencyRequestHandler<T> statefulRequestHandler(
            final Method method,
            Class<?> module,
            final RequestVisitor moduleRequestVisitor,
            final ModuleMetadata moduleMetadata,
            GraphContext context) {

        int modifiers = method.getModifiers();

        if (Modifier.isStatic(modifiers)) {
            throw new IllegalArgumentException("A stateful module's provider methods must not be static.  The method ["
                    + method.getName() + "] in module [" + module.getName() + "] is static.");
        }

        if (method.getReturnType() == void.class) {
            throw new IllegalArgumentException("A module's provider methods cannot have a void return type.  The method ["
                    + method.getName() + "] in module [" + module.getName() + "] is returns void.");
        }

        ComponentMetadata<Method> componentMetadata = componentMetadataResolver.resolveMetadata(method, moduleMetadata);

        // TODO targeted return type check
        final List<Dependency<? super T>> dependencies = Collections.<Dependency<? super T>>singletonList(
                Dependency.from(componentMetadata.qualifier(), method.getGenericReturnType()));

        return requestHandler(
                componentAdapterFactory.<T>withStatefulMethodProvider(componentMetadata, context),
                dependencies,
                moduleRequestVisitor,
                AccessFilter.create(method));
    }

    private <T> DependencyRequestHandler<T> requestHandler(
                                                     final ComponentAdapter<T> componentAdapter,
                                                     final List<Dependency<? super T>> targets,
                                                     final RequestVisitor moduleRequestVisitor,
                                                     final AccessFilter<Class<?>> classAccessFilter) {

        RequestVisitor scopeVisitor = (dependencyRequest, response) -> {
            if (!componentAdapter.metadata().scope().canInject(dependencyRequest.sourceScope())) {
                response.addError("mis-scoped"); // TODO message
            }
        };

        return new DependencyRequestHandler<T>() {

            @Override public List<Dependency<? super T>> targets() {
                return targets;
            }

            @Override public List<Dependency<?>> dependencies() {
                return componentAdapter.dependencies();
            }

            @Override public DependencyResponse<T> handle(DependencyRequest<? super T> dependencyRequest) {
                MutableDependencyResponse<T> response =
                        new DependencyResponseImpl<>(dependencyRequest, componentAdapter.provisionStrategy());
                moduleRequestVisitor.visit(dependencyRequest, response);
                scopeVisitor.visit(dependencyRequest, response);
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

            @Override public DependencyRequestHandler<T> replicate(GraphContext context) {
                return requestHandler(
                        componentAdapter.replicate(context),
                        targets,
                        moduleRequestVisitor,
                        classAccessFilter);
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

        RequestVisitor NONE = (dependencyRequest, dependencyResponse) -> { };

        void visit(DependencyRequest<?> dependencyRequest, MutableDependencyResponse<?> dependencyResponse);
    }

    private static class DependencyResponseImpl<T> implements MutableDependencyResponse<T> {

        List<String> errors;
        final DependencyRequest<? super T> dependencyRequest;
        final ProvisionStrategy<? extends T> provisionStrategy;

        DependencyResponseImpl(DependencyRequest<? super T> dependencyRequest,
                               ProvisionStrategy<T> provisionStrategy) {
            this.dependencyRequest = dependencyRequest;
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
                StringBuilder stringBuilder = new StringBuilder("There were errors processing a dependency:\n");
                stringBuilder.append("    Request by:            ").append(dependencyRequest.source()).append("\n");
                stringBuilder.append("    Dependency requested:  ").append(dependencyRequest.dependency()).append("\n");
                stringBuilder.append("    ").append("Reasons for failure: \n");
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
