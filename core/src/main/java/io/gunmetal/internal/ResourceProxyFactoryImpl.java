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
import io.gunmetal.spi.ResourceMetadata;
import io.gunmetal.spi.ResourceMetadataResolver;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.QualifierResolver;
import io.gunmetal.spi.Scopes;
import io.gunmetal.spi.TypeKey;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author rees.byars
 */
class ResourceProxyFactoryImpl implements ResourceProxyFactory {

    private final ResourceFactory resourceFactory;
    private final QualifierResolver qualifierResolver;
    private final ResourceMetadataResolver resourceMetadataResolver;
    private final boolean requireExplicitModuleDependencies;

    ResourceProxyFactoryImpl(ResourceFactory resourceFactory,
                             QualifierResolver qualifierResolver,
                             ResourceMetadataResolver resourceMetadataResolver,
                             boolean requireExplicitModuleDependencies) {
        this.resourceFactory = resourceFactory;
        this.qualifierResolver = qualifierResolver;
        this.resourceMetadataResolver = resourceMetadataResolver;
        this.requireExplicitModuleDependencies = requireExplicitModuleDependencies;
    }

    @Override public List<ResourceProxy<?>> createProxiesForModule(final Class<?> module,
                                                                   GraphContext context,
                                                                   Set<Class<?>> loadedModules) {
        if (loadedModules.contains(module)) {
            return Collections.emptyList();
        }
        loadedModules.add(module);

        final Module moduleAnnotation = module.getAnnotation(Module.class);
        if (moduleAnnotation == null) {
            context.errors().add("The module class [" + module.getName()
                    + "] must be annotated with @Module()");
            return Collections.emptyList();
        }
        final RequestVisitor moduleRequestVisitor = moduleRequestVisitor(module, moduleAnnotation, context);
        final ModuleMetadata moduleMetadata = moduleMetadata(module, moduleAnnotation, context);
        final List<ResourceProxy<?>> resourceProxies = new ArrayList<>();
        addResourceProxies(
                moduleAnnotation,
                module,
                resourceProxies,
                moduleMetadata,
                moduleRequestVisitor,
                context,
                loadedModules);
        return resourceProxies;

    }

    @Override public <T> ResourceProxy<T> createJitProxyForRequest(
            DependencyRequest<T> dependencyRequest, GraphContext context) {
        Dependency<T> dependency = dependencyRequest.dependency();
        TypeKey<T> typeKey = dependency.typeKey();
        if (!isInstantiable(typeKey.raw())) {
            return null;
        }
        Class<? super T> cls = typeKey.raw();
        ModuleMetadata moduleMetadata = dependencyRequest.sourceModule(); // essentially, same as library
        Resource<T> resource = resourceFactory.withClassProvider(
                resourceMetadataResolver.resolveMetadata(cls, moduleMetadata, context.errors()), context);
        if (!resource.metadata().qualifier().equals(dependency.qualifier())) {
            return null;
        }
        return resourceProxy(
                resource,
                Collections.<Dependency<? super T>>singletonList(dependency),
                moduleMetadata.moduleAnnotation() == Module.NONE ?
                        RequestVisitor.NONE :
                        moduleRequestVisitor(moduleMetadata.moduleClass(), moduleMetadata.moduleAnnotation(), context),
                decorateForModule(moduleMetadata, AccessFilter.create(cls)),
                context);
    }

    private RequestVisitor moduleRequestVisitor(final Class<?> module,
                                                final Module moduleAnnotation,
                                                GraphContext context) {
        final RequestVisitor blackListVisitor = blackListVisitor(module, moduleAnnotation, context);
        final RequestVisitor whiteListVisitor = whiteListVisitor(module, moduleAnnotation, context);
        final RequestVisitor dependsOnVisitor = dependsOnVisitor(module);
        final AccessFilter<Class<?>> classAccessFilter = AccessFilter.create(moduleAnnotation.access(), module);
        final RequestVisitor moduleClassVisitor = (dependencyRequest, response) -> {
            if (!classAccessFilter.isAccessibleTo(dependencyRequest.sourceModule().moduleClass())) {
                response.addError(
                        "The module [" + dependencyRequest.sourceModule().moduleClass().getName()
                                + "] does not have access to [" + classAccessFilter.filteredElement() + "]"
                );
            }
        };
        return (dependencyRequest, response) -> {
            moduleClassVisitor.visit(dependencyRequest, response);
            dependsOnVisitor.visit(dependencyRequest, response);
            blackListVisitor.visit(dependencyRequest, response);
            whiteListVisitor.visit(dependencyRequest, response);
        };
    }

    private ModuleMetadata moduleMetadata(final Class<?> module, final Module moduleAnnotation, GraphContext context) {
        final Qualifier qualifier = qualifierResolver.resolve(module, context.errors());
        return new ModuleMetadata(module, qualifier, moduleAnnotation);
    }

    private RequestVisitor blackListVisitor(final Class<?> module, Module moduleAnnotation, GraphContext context) {

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

        final Qualifier blackListQualifier = qualifierResolver.resolve(blackListConfigClass, context.errors());

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
                    && dependencyRequest.sourceQualifier().qualifiers().length > 0
                    && dependencyRequest.sourceQualifier().intersects(blackListQualifier);

            if (qualifierMatch) {
                response.addError("The module [" + requestingSourceModuleClass.getName()
                        + "] does not have access to the module [" + module.getName() + "].");
            }
        };
    }

    private RequestVisitor whiteListVisitor(final Class<?> module, Module moduleAnnotation, GraphContext context) {

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

        final Qualifier whiteListQualifier = qualifierResolver.resolve(whiteListConfigClass, context.errors());

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

            if (requestSourceModule.referencedModules().length == 0 && !requireExplicitModuleDependencies) {
                return;
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

    private void addResourceProxies(Module moduleAnnotation,
                                    Class<?> module,
                                    List<ResourceProxy<?>> resourceProxies,
                                    ModuleMetadata moduleMetadata,
                                    RequestVisitor moduleRequestVisitor,
                                    GraphContext context,
                                    Set<Class<?>> loadedModules) {
        if (!module.isInterface() && module.getSuperclass() != Object.class) {
            context.errors().add("The module " + module.getName() + " extends a class other than Object");
        }
        if (moduleAnnotation.stateful()) {
            if (!moduleAnnotation.provided()) {
                resourceProxies.add(managedModuleResourceProxy(module, moduleMetadata, context));
            } else {
                resourceProxies.add(providedModuleResourceProxy(module, moduleMetadata, context));
            }
            Arrays.stream(module.getDeclaredMethods()).filter(m -> !m.isSynthetic()).forEach(m -> {
                resourceProxies.add(statefulResourceProxy(m, module, moduleRequestVisitor, moduleMetadata, context));
            });
        } else {
            Arrays.stream(module.getDeclaredMethods()).filter(m -> !m.isSynthetic()).forEach(m -> {
                ResourceMetadata<Method> resourceMetadata =
                        resourceMetadataResolver.resolveMetadata(m, moduleMetadata, context.errors());
                if (resourceMetadata.isProvider()) {
                    resourceProxies.add(
                            resourceProxy(resourceMetadata, module, moduleRequestVisitor, moduleMetadata, context));
                }
            });
        }
        for (Class<?> library : moduleAnnotation.subsumes()) {
            Module libModule = library.getAnnotation(Module.class);
            // TODO allow provided? require prototype?
            Qualifier libQualifier = qualifierResolver.resolve(library, context.errors());
            if (libModule == null) {
                context.errors().add("A class without @Module cannot be subsumed");
            } else if (!libModule.lib()) {
                context.errors().add("@Module.lib must be true to be subsumed");
            } else if (libQualifier != Qualifier.NONE) {
                context.errors().add("Library " + library.getName() + " should not have a qualifier -> " + libQualifier);
            }
            addResourceProxies(
                    libModule,
                    library,
                    resourceProxies,
                    moduleMetadata,
                    moduleRequestVisitor,
                    context,
                    loadedModules);
        }
        for (Class<?> m : moduleAnnotation.dependsOn()) {
            resourceProxies.addAll(createProxiesForModule(m, context, loadedModules));
        }
    }

    private AccessFilter<Class<?>> decorateForModule(ModuleMetadata moduleMetadata,
                                                     AccessFilter<Class<?>> accessFilter) {
        return new AccessFilter<Class<?>>() {
            @Override public AnnotatedElement filteredElement() {
                return accessFilter.filteredElement();
            }
            @Override public boolean isAccessibleTo(Class<?> target) {
                // supports library access
                return target == moduleMetadata.moduleClass() || accessFilter.isAccessibleTo(target);
            }
        };
    }

    private <T> ResourceProxy<T> resourceProxy(
            ResourceMetadata<Method> resourceMetadata,
            Class<?> module,
            RequestVisitor moduleRequestVisitor,
            ModuleMetadata moduleMetadata,
            GraphContext context) {

        Method method = resourceMetadata.provider();

        int modifiers = method.getModifiers();

        if (module.isInterface() && !Modifier.isStatic(modifiers)) {
            throw new IllegalArgumentException("A module's provider methods must be static.  The method ["
                    + method.getName() + "] in module [" + module.getName() + "] is not static.");
        }

        if (method.getReturnType() == void.class) {
            throw new IllegalArgumentException("A module's provider methods cannot have a void return type.  The method ["
                    + method.getName() + "] in module [" + module.getName() + "] is returns void.");
        }

        // if (resourceMetadata.isModule()) {
            // TODO
        // }

        // TODO targeted return type check
        final List<Dependency<? super T>> dependencies = Collections.<Dependency<? super T>>singletonList(
                Dependency.from(resourceMetadata.qualifier(), method.getGenericReturnType()));

        return resourceProxy(
                resourceFactory.<T>withMethodProvider(resourceMetadata, context),
                dependencies,
                moduleRequestVisitor,
                decorateForModule(moduleMetadata, AccessFilter.create(method)),
                context);
    }

    private <T> ResourceProxy<T> statefulResourceProxy(
            final Method method,
            Class<?> module,
            final RequestVisitor moduleRequestVisitor,
            final ModuleMetadata moduleMetadata,
            GraphContext context) {

        if (method.getReturnType() == void.class) {
            throw new IllegalArgumentException("A module's provider methods cannot have a void return type.  The method ["
                    + method.getName() + "] in module [" + module.getName() + "] is returns void.");
        }

        ResourceMetadata<Method> resourceMetadata =
                resourceMetadataResolver.resolveMetadata(method, moduleMetadata, context.errors());

        Dependency<T> provisionDependency =
                Dependency.from(resourceMetadata.qualifier(), method.getGenericReturnType());

        Dependency<?> moduleDependency =
                Dependency.from(moduleMetadata.qualifier(), module);

        // TODO targeted return type check
        final List<Dependency<? super T>> dependencies =
                Collections.<Dependency<? super T>>singletonList(provisionDependency);

        if (Modifier.isStatic(resourceMetadata.provider().getModifiers())) {
            return resourceProxy(
                    resourceFactory.<T>withMethodProvider(resourceMetadata, context),
                    dependencies,
                    moduleRequestVisitor,
                    decorateForModule(moduleMetadata, AccessFilter.create(method)),
                    context);
        }

        return resourceProxy(
                resourceFactory.<T>withStatefulMethodProvider(resourceMetadata, moduleDependency, context),
                dependencies,
                moduleRequestVisitor,
                decorateForModule(moduleMetadata, AccessFilter.create(method)),
                context);
    }

    private <T> ResourceProxy<T> managedModuleResourceProxy(Class<T> module,
                                                                        ModuleMetadata moduleMetadata,
                                                                        GraphContext context) {
        Dependency<T> dependency = Dependency.from(moduleMetadata.qualifier(), module);
        Resource<T> resource = resourceFactory.withClassProvider(
                resourceMetadataResolver.resolveMetadata(module, moduleMetadata, context.errors()), context);
        return resourceProxy(
                resource,
                Collections.<Dependency<? super T>>singletonList(dependency),
                (dependencyRequest, dependencyResponse) -> {
                    if (!dependencyRequest.sourceModule().equals(moduleMetadata)) {
                        dependencyResponse.addError("Module can only be requested by its providers"); // TODO
                    }
                },
                decorateForModule(moduleMetadata, AccessFilter.create(module)),
                context);
    }

    private <T> ResourceProxy<T> providedModuleResourceProxy(Class<T> module,
                                                                        ModuleMetadata moduleMetadata,
                                                                        GraphContext context) {
        Dependency<T> dependency = Dependency.from(moduleMetadata.qualifier(), module);
        ResourceMetadata<Class<?>> resourceMetadata =
                resourceMetadataResolver.resolveMetadata(module, moduleMetadata, context.errors());
        if (resourceMetadata.scope() != Scopes.SINGLETON) {
            // TODO context.errors().add(resourceMetadata, "Provided modules must have a scope of singleton");
        }
        Resource<T> resource = resourceFactory.withProvidedModule(
                resourceMetadata, context);
        return resourceProxy(
                resource,
                Collections.<Dependency<? super T>>singletonList(dependency),
                (dependencyRequest, dependencyResponse) -> {
                    if (!dependencyRequest.sourceModule().equals(moduleMetadata)) {
                        dependencyResponse.addError("Module can only be requested by its providers"); // TODO
                    }
                },
                decorateForModule(moduleMetadata, AccessFilter.create(module)),
                context);
    }

    private <T> ResourceProxy<T> resourceProxy(
                                                     final Resource<T> resource,
                                                     final List<Dependency<? super T>> targets,
                                                     final RequestVisitor moduleRequestVisitor,
                                                     final AccessFilter<Class<?>> classAccessFilter,
                                                     GraphContext context) {

        RequestVisitor scopeVisitor = (dependencyRequest, response) -> {
            if (!resource.metadata().scope().canInject(dependencyRequest.sourceScope())) {
                response.addError("mis-scoped"); // TODO message
            }
        };

        return new ResourceProxy<T>() {

            @Override public List<Dependency<? super T>> targets() {
                return targets;
            }

            @Override public List<Dependency<?>> dependencies() {
                return resource.dependencies();
            }

            @Override public DependencyResponse<T> service(DependencyRequest<? super T> dependencyRequest) {
                DependencyResponseImpl<T> response =
                        new DependencyResponseImpl<>(dependencyRequest, resource.provisionStrategy(), context);
                moduleRequestVisitor.visit(dependencyRequest, response);
                scopeVisitor.visit(dependencyRequest, response);
                if (!classAccessFilter.isAccessibleTo(dependencyRequest.sourceModule().moduleClass())) {
                    response.addError(
                            "The class [" + dependencyRequest.sourceOrigin().getName()
                            + "] does not have access to [" + classAccessFilter.filteredElement() + "]"
                    );
                }
                response.validateResponse();
                return response;
            }

            @Override public ProvisionStrategy<T> force() {
                return resource.provisionStrategy();
            }

            @Override public ResourceMetadata<?> resourceMetadata() {
                return resource.metadata();
            }

            @Override public ResourceProxy<T> replicateWith(GraphContext context) {
                return resourceProxy(
                        resource.replicateWith(context),
                        targets,
                        moduleRequestVisitor,
                        classAccessFilter,
                        context);
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
        final GraphContext context;

        DependencyResponseImpl(DependencyRequest<? super T> dependencyRequest,
                               ProvisionStrategy<T> provisionStrategy,
                               GraphContext context) {
            this.dependencyRequest = dependencyRequest;
            this.provisionStrategy = provisionStrategy;
            this.context = context;
        }

        @Override public void addError(String errorMessage) {
            if (errors == null) {
                errors = new LinkedList<>();
            }
            errors.add(errorMessage);
        }

        @Override public ProvisionStrategy<? extends T> provisionStrategy() {
            return provisionStrategy;
        }

        void validateResponse() {
            if (errors != null) {
                for (String error : errors) {
                    context.errors().add(
                            dependencyRequest.sourceProvision(),
                            "Denied request for " + dependencyRequest.dependency() + ".  Reason -> " + error);
                }
            }
        }
    }

}
