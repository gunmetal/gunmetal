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

import io.gunmetal.Module;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.QualifierResolver;
import io.gunmetal.spi.ResourceMetadata;
import io.gunmetal.spi.ResourceMetadataResolver;
import io.gunmetal.spi.Scopes;
import io.gunmetal.spi.TypeKey;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author rees.byars
 */
class BindingFactoryImpl implements BindingFactory {

    private final ResourceFactory resourceFactory;
    private final QualifierResolver qualifierResolver;
    private final ResourceMetadataResolver resourceMetadataResolver;

    BindingFactoryImpl(ResourceFactory resourceFactory,
                       QualifierResolver qualifierResolver,
                       ResourceMetadataResolver resourceMetadataResolver) {
        this.resourceFactory = resourceFactory;
        this.qualifierResolver = qualifierResolver;
        this.resourceMetadataResolver = resourceMetadataResolver;
    }

    @Override public List<Binding> createBindingsForModule(final Class<?> module,
                                                           ComponentContext context,
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
        final ModuleMetadata moduleMetadata = moduleMetadata(module, moduleAnnotation, context);
        final List<Binding> resourceBindings = new ArrayList<>();
        addResourceBindings(
                moduleAnnotation,
                module,
                resourceBindings,
                moduleMetadata,
                context,
                loadedModules);
        return resourceBindings;

    }

    @Override public Binding createJitBindingForRequest(
            DependencyRequest dependencyRequest, ComponentContext context) {
        Dependency dependency = dependencyRequest.dependency();
        TypeKey typeKey = dependency.typeKey();
        if (!isInstantiable(typeKey.raw())) {
            return null;
        }
        Class<?> cls = typeKey.raw();
        ModuleMetadata moduleMetadata = dependencyRequest.sourceModule(); // essentially, same as library
        Resource resource = resourceFactory.withClassProvider(
                resourceMetadataResolver.resolveMetadata(cls, moduleMetadata, context.errors()), context);
        if (!resource.metadata().qualifier().equals(dependency.qualifier())) {
            return null;
        }
        return new BindingImpl(resource, Collections.singletonList(dependency));
    }

    @Override public List<Binding> createJitFactoryBindingsForRequest(
            DependencyRequest dependencyRequest, ComponentContext context) {
        ModuleMetadata moduleMetadata = dependencyRequest.sourceModule(); // TODO
        final List<Binding> resourceBindings = new ArrayList<>();
        addResourceBindings(
                Module.NONE,  // TODO hmmm
                dependencyRequest.dependency().typeKey().raw(), // TODO hmmmm
                resourceBindings,
                moduleMetadata,
                context,
                Collections.emptySet()); // TODO is this okay?
        return resourceBindings;
    }

    private ModuleMetadata moduleMetadata(final Class<?> module, final Module moduleAnnotation, ComponentContext context) {
        final Qualifier qualifier = qualifierResolver.resolve(module, context.errors());
        return new ModuleMetadata(module, qualifier, moduleAnnotation);
    }

    private void addResourceBindings(Module moduleAnnotation,
                                     Class<?> module,
                                     List<Binding> resourceBindings,
                                     ModuleMetadata moduleMetadata,
                                     ComponentContext context,
                                     Set<Class<?>> loadedModules) {

        if (!module.isInterface() && module.getSuperclass() != Object.class && !module.isPrimitive()) {
            context.errors().add("The module " + module.getName() + " extends a class other than Object");
        }

        if (moduleAnnotation.stateful()) {
            if (!moduleAnnotation.provided()) {
                resourceBindings.add(managedModuleResourceBinding(module, moduleMetadata, context));
            } else {
                resourceBindings.add(providedModuleResourceBinding(module, moduleMetadata, context));
            }
        }

        Dependency moduleDependency =
                Dependency.from(moduleMetadata.qualifier(), module);

        Arrays.stream(module.getDeclaredFields()).filter(f -> !f.isSynthetic()).forEach(f -> {
            ResourceMetadata<Field> resourceMetadata =
                    resourceMetadataResolver.resolveMetadata(f, moduleMetadata, context.errors());
            if (resourceMetadata.isProvider()) {
                List<Dependency> dependencies = Collections.singletonList(
                        Dependency.from(resourceMetadata.qualifier(), f.getGenericType()));
                resourceBindings.add(new BindingImpl(
                        resourceFactory.withFieldProvider(resourceMetadata, moduleDependency, context),
                        dependencies));
            }
        });

        Arrays.stream(module.getDeclaredMethods()).filter(m -> !m.isSynthetic()).forEach(m -> {
            ResourceMetadata<Method> resourceMetadata =
                    resourceMetadataResolver.resolveMetadata(m, moduleMetadata, context.errors());
            if (resourceMetadata.isProvider()) {
                if (m.getReturnType() == void.class) {
                    throw new IllegalArgumentException("A module's provider methods cannot have a void return type.  The method ["
                            + m.getName() + "] in module [" + module.getName() + "] is returns void.");
                }
                List<Dependency> dependencies = Collections.singletonList(
                        Dependency.from(resourceMetadata.qualifier(), m.getGenericReturnType()));
                resourceBindings.add(new BindingImpl(
                        resourceFactory.withMethodProvider(resourceMetadata, moduleDependency, context),
                        dependencies));
            }
        });

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
            addResourceBindings(
                    libModule,
                    library,
                    resourceBindings,
                    moduleMetadata,
                    context,
                    loadedModules);
        }
        for (Class<?> m : moduleAnnotation.dependsOn()) {
            resourceBindings.addAll(createBindingsForModule(m, context, loadedModules));
        }
    }

    private Binding managedModuleResourceBinding(Class<?> module,
                                                 ModuleMetadata moduleMetadata,
                                                 ComponentContext context) {
        Dependency dependency = Dependency.from(moduleMetadata.qualifier(), module);
        Resource resource = resourceFactory.withClassProvider(
                resourceMetadataResolver.resolveMetadata(module, moduleMetadata, context.errors()), context);
        return new BindingImpl(
                resource,
                Collections.singletonList(dependency));
    }

    private Binding providedModuleResourceBinding(Class<?> module,
                                                  ModuleMetadata moduleMetadata,
                                                  ComponentContext context) {
        Dependency dependency = Dependency.from(moduleMetadata.qualifier(), module);
        ResourceMetadata<Class<?>> resourceMetadata =
                resourceMetadataResolver.resolveMetadata(module, moduleMetadata, context.errors());
        if (resourceMetadata.scope() != Scopes.SINGLETON) {
            // TODO context.errors().add(resourceMetadata, "Provided modules must have a scope of singleton");
        }
        Resource resource = resourceFactory.withProvidedModule(
                resourceMetadata, context);
        return new BindingImpl(
                resource,
                Collections.singletonList(dependency));
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

}
