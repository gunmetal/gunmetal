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

import io.gunmetal.spi.ClassWalker;
import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.QualifierResolver;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.spi.ResourceMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
class InjectorFactoryImpl implements InjectorFactory {

    private final QualifierResolver qualifierResolver;
    private final ConstructorResolver constructorResolver;
    private final ClassWalker classWalker;

    InjectorFactoryImpl(QualifierResolver qualifierResolver,
                        ConstructorResolver constructorResolver,
                        ClassWalker classWalker) {
        this.qualifierResolver = qualifierResolver;
        this.constructorResolver = constructorResolver;
        this.classWalker = classWalker;
    }

    @Override public Injector compositeInjector(final ResourceMetadata<Class<?>> resourceMetadata,
                                                final GraphContext context) {

        final List<Injector> injectors = new ArrayList<>();

        classWalker.walk(resourceMetadata.provider(),
                field -> {
                    Dependency dependency = Dependency.from(
                            qualifierResolver.resolveDependencyQualifier(
                                    field,
                                    resourceMetadata.moduleMetadata().qualifier(),
                                    (error) -> context.errors().add(resourceMetadata, error)),
                            field.getGenericType());
                    injectors.add(new FieldInjector(field, resourceMetadata, dependency, context.linkers()));
                },
                method -> {
                    ParameterizedFunction function = new MethodFunction(method);
                    injectors.add(new FunctionInjector(
                            function,
                            resourceMetadata,
                            dependenciesForFunction(
                                    resourceMetadata,
                                    function,
                                    qualifierResolver,
                                    context),
                            context.linkers()));
                },
                resourceMetadata,
                (error) -> context.errors().add(resourceMetadata, error));

        return new CompositeInjector(injectors);

    }

    @Override public Injector lazyCompositeInjector(final ResourceMetadata<?> resourceMetadata,
                                                    GraphContext context) {
        return new LazyCompositeInjector(classWalker, qualifierResolver, resourceMetadata, context);
    }

    @Override public Instantiator constructorInstantiator(ResourceMetadata<Class<?>> resourceMetadata,
                                                          GraphContext context) {
        Constructor<?> constructor = constructorResolver.resolve(resourceMetadata.provider());
        ParameterizedFunction function = new ConstructorFunction(constructor);
        Injector injector = new FunctionInjector(
                function,
                resourceMetadata,
                dependenciesForFunction(
                        resourceMetadata,
                        function,
                        qualifierResolver,
                        context),
                context.linkers());
        return new InstantiatorImpl(injector);
    }

    @Override public Instantiator instanceInstantiator(ResourceMetadata<Class<?>> resourceMetadata,
                                                       GraphContext context) {
        return new ProvidedModuleInstantiator(resourceMetadata.providerClass());
    }

    @Override public Instantiator methodInstantiator(
            ResourceMetadata<Method> resourceMetadata, Dependency moduleDependency, GraphContext context) {
        Method provider = resourceMetadata.provider();
        if (!Modifier.isStatic(provider.getModifiers())) {
            ParameterizedFunction function = new MethodFunction(resourceMetadata.provider());
            FunctionInjector injector = new FunctionInjector(
                    function,
                    resourceMetadata,
                    dependenciesForFunction(
                            resourceMetadata,
                            function,
                            qualifierResolver,
                            context),
                    context.linkers());
            return new StatefulInstantiator(injector, resourceMetadata, moduleDependency);
        }
        ParameterizedFunction function = new MethodFunction(resourceMetadata.provider());
        Injector injector = new FunctionInjector(
                function,
                resourceMetadata,
                dependenciesForFunction(
                        resourceMetadata,
                        function,
                        qualifierResolver,
                        context),
                context.linkers());
        return new InstantiatorImpl(injector);
    }

    @Override public Instantiator fieldInstantiator(
            ResourceMetadata<Field> resourceMetadata, Dependency moduleDependency, GraphContext context) {
        Field provider = resourceMetadata.provider();
        if (!Modifier.isStatic(provider.getModifiers())) {
            return new StatefulInstantiator(
                    new ReverseFieldInjector(provider), resourceMetadata,  moduleDependency);
        }
        return new InstantiatorImpl(new ReverseFieldInjector(resourceMetadata.provider()));
    }

    private static Dependency[] dependenciesForFunction(ResourceMetadata<?> resourceMetadata,
                                                        ParameterizedFunction function,
                                                        QualifierResolver qualifierResolver,
                                                        GraphContext context) {
        Dependency[] dependencies = new Dependency[function.getParameterTypes().length];
        for (int i = 0; i < dependencies.length; i++) {
            Parameter parameter = new Parameter(function, i);
            dependencies[i] = Dependency.from(
                    qualifierResolver.resolveDependencyQualifier(
                            parameter,
                            resourceMetadata.moduleMetadata().qualifier(),
                            (error) -> context.errors().add(resourceMetadata, error)),
                    parameter.type);
        }
        return dependencies;
    }

    private interface ParameterizedFunction {
        Object invoke(Object onInstance, Object[] params)
                throws InvocationTargetException, IllegalAccessException, InstantiationException;

        Type[] getParameterTypes();

        Annotation[][] getParameterAnnotations();
    }

    private static class MethodFunction implements ParameterizedFunction {
        final Method method;

        MethodFunction(Method method) {
            method.setAccessible(true);
            this.method = method;
        }

        @Override public Object invoke(Object onInstance, Object[] params)
                throws InvocationTargetException, IllegalAccessException {
            return method.invoke(onInstance, params);
        }

        @Override public Type[] getParameterTypes() {
            return method.getGenericParameterTypes();
        }

        @Override public Annotation[][] getParameterAnnotations() {
            return method.getParameterAnnotations();
        }
    }

    private static class ConstructorFunction implements ParameterizedFunction {
        final Constructor<?> constructor;

        ConstructorFunction(Constructor<?> constructor) {
            constructor.setAccessible(true);
            this.constructor = constructor;
        }

        @Override public Object invoke(Object onInstance, Object[] params)
                throws InvocationTargetException, IllegalAccessException, InstantiationException {
            return constructor.newInstance(params);
        }

        @Override public Type[] getParameterTypes() {
            return constructor.getGenericParameterTypes();
        }

        @Override public Annotation[][] getParameterAnnotations() {
            return constructor.getParameterAnnotations();
        }
    }

    private static class Parameter implements AnnotatedElement {

        final Annotation[] annotations;
        final Type type;

        Parameter(ParameterizedFunction parameterizedFunction, int index) {
            annotations = parameterizedFunction.getParameterAnnotations()[index];
            type = parameterizedFunction.getParameterTypes()[index];
        }

        @Override public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            for (Annotation annotation : annotations) {
                if (annotationClass.isInstance(annotation)) {
                    return annotationClass.cast(annotation);
                }
            }
            return null;
        }

        @Override public Annotation[] getAnnotations() {
            return annotations;
        }

        @Override public Annotation[] getDeclaredAnnotations() {
            return annotations;
        }

    }

    private static class FieldInjector implements Injector {

        private final Field field;
        private final ResourceMetadata<?> resourceMetadata;
        private final Dependency dependency;
        private ProvisionStrategy provisionStrategy;

        FieldInjector(Field field,
                      final ResourceMetadata<?> resourceMetadata,
                      final Dependency dependency,
                      Linkers linkers) {
            field.setAccessible(true);
            linkers.addWiringLinker((internalProvider, linkingContext) ->
                    provisionStrategy = internalProvider.getProvisionStrategy(
                            DependencyRequest.create(
                                    resourceMetadata,
                                    dependency)));
            this.field = field;
            this.resourceMetadata = resourceMetadata;
            this.dependency = dependency;
        }

        FieldInjector(Field field,
                      ResourceMetadata<?> resourceMetadata,
                      Dependency dependency,
                      ProvisionStrategy provisionStrategy) {
            field.setAccessible(true);
            this.field = field;
            this.resourceMetadata = resourceMetadata;
            this.dependency = dependency;
            this.provisionStrategy = provisionStrategy;
        }

        @Override public Object inject(Object target,
                                       InternalProvider internalProvider,
                                       ResolutionContext resolutionContext) {
            try {
                field.set(target, provisionStrategy.get(internalProvider, resolutionContext));
                return null;
            } catch (IllegalAccessException e) {
                throw new RuntimeException("An unexpected exception has occurred", e);
            }
        }

        @Override public Injector replicateWith(GraphContext context) {
            return new FieldInjector(field, resourceMetadata, dependency, context.linkers());
        }

        @Override public List<Dependency> dependencies() {
            return Collections.singletonList(dependency);
        }

    }

    private static class ReverseFieldInjector implements Injector {

        private final Field field;

        ReverseFieldInjector(Field field) {
            field.setAccessible(true);
            this.field = field;
        }

        @Override public Injector replicateWith(GraphContext context) {
            return new ReverseFieldInjector(field);
        }

        @Override public Object inject(Object target, InternalProvider internalProvider, ResolutionContext resolutionContext) {
            try {
                return field.get(target);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("An unexpected exception has occurred", e);
            }
        }

        @Override public List<Dependency> dependencies() {
            return Collections.emptyList();
        }

    }

    private static class FunctionInjector implements Injector {

        private final ParameterizedFunction function;
        private final ResourceMetadata<?> resourceMetadata;
        private final Dependency[] dependencies;
        private final ProvisionStrategy[] provisionStrategies;

        FunctionInjector(final ParameterizedFunction function,
                         final ResourceMetadata<?> resourceMetadata,
                         final Dependency[] dependencies,
                         final Linkers linkers) {
            this.function = function;
            this.resourceMetadata = resourceMetadata;
            this.dependencies = dependencies;
            provisionStrategies = new ProvisionStrategy[dependencies.length];
            linkers.addWiringLinker((internalProvider, linkingContext) -> {
                for (int i = 0; i < dependencies.length; i++) {
                    provisionStrategies[i] = internalProvider.getProvisionStrategy(
                                DependencyRequest.create(resourceMetadata, dependencies[i]));
                }
            });
        }

        FunctionInjector(ParameterizedFunction function,
                         ResourceMetadata<?> resourceMetadata,
                         Dependency[] dependencies,
                         ProvisionStrategy[] provisionStrategies) {
            this.function = function;
            this.resourceMetadata = resourceMetadata;
            this.dependencies = dependencies;
            this.provisionStrategies = provisionStrategies;
        }

        @Override public Object inject(Object target,
                                       InternalProvider internalProvider,
                                       ResolutionContext resolutionContext) {
            Object[] parameters = new Object[provisionStrategies.length];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = provisionStrategies[i].get(internalProvider, resolutionContext);
            }
            try {
                return function.invoke(target, parameters);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException  e) {
                if (e.getCause() != null && e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        }

        @Override public Injector replicateWith(GraphContext context) {
            return new FunctionInjector(function, resourceMetadata, dependencies, context.linkers());
        }

        @Override public List<Dependency> dependencies() {
            return Arrays.asList(dependencies);
        }

    }

    private static class CompositeInjector implements Injector {

        private final List<Injector> injectors;

        CompositeInjector(List<Injector> injectors) {
            this.injectors = injectors;
        }

        @Override public Object inject(Object target,
                                       InternalProvider internalProvider,
                                       ResolutionContext resolutionContext) {
            if (!injectors.isEmpty()) {
                for (Injector injector : injectors) {
                    injector.inject(target, internalProvider, resolutionContext);
                }
            }
            return null;
        }

        @Override public Injector replicateWith(GraphContext context) {
            List<Injector> newInjectors = new ArrayList<>();
            for (Injector injector : injectors) {
                newInjectors.add(injector.replicateWith(context));
            }
            return new CompositeInjector(newInjectors);
        }

        @Override public List<Dependency> dependencies() {
            List<Dependency> dependencies = new LinkedList<>();
            for (Injector injector : injectors) {
                dependencies.addAll(injector.dependencies());
            }
            return dependencies;
        }

    }

    private static class LazyCompositeInjector implements Injector {

        private final ClassWalker classWalker;
        private final QualifierResolver qualifierResolver;
        private final ResourceMetadata<?> resourceMetadata;
        private final GraphContext context;
        private volatile List<Injector> injectors;

        LazyCompositeInjector(ClassWalker classWalker,
                              QualifierResolver qualifierResolver,
                              ResourceMetadata<?> resourceMetadata,
                              GraphContext context) {
            this.classWalker = classWalker;
            this.qualifierResolver = qualifierResolver;
            this.resourceMetadata = resourceMetadata;
            this.context = context;
        }

        void init(Class<?> targetClass, final InternalProvider internalProvider) {

            injectors = new ArrayList<>();

            classWalker.walk(targetClass,
                    field -> {
                        Dependency dependency = Dependency.from(
                                qualifierResolver.resolveDependencyQualifier(
                                        field,
                                        resourceMetadata.moduleMetadata().qualifier(),
                                        (error) -> context.errors().add(resourceMetadata, error)),
                                field.getGenericType());
                        ProvisionStrategy provisionStrategy = internalProvider.getProvisionStrategy(
                                DependencyRequest.create(resourceMetadata, dependency));
                        injectors.add(new FieldInjector(field, resourceMetadata, dependency, provisionStrategy));
                    },
                    method -> {
                        ParameterizedFunction function = new MethodFunction(method);
                        Dependency[] dependencies =
                                dependenciesForFunction(
                                        resourceMetadata,
                                        function,
                                        qualifierResolver,
                                        context);
                        ProvisionStrategy[] provisionStrategies = new ProvisionStrategy[dependencies.length];
                        for (int i = 0; i < dependencies.length; i++) {
                            provisionStrategies[i] = internalProvider.getProvisionStrategy(
                                    DependencyRequest.create(resourceMetadata, dependencies[i]));
                        }
                        injectors.add(new FunctionInjector(
                                function,
                                resourceMetadata,
                                dependencies,
                                provisionStrategies));
                    },
                    resourceMetadata,
                    (error) -> context.errors().add(resourceMetadata, error));
        }

        @Override public Object inject(
                Object target, InternalProvider internalProvider, ResolutionContext resolutionContext) {
            if (injectors == null) {
                synchronized (this) {
                    if (injectors == null) {
                        init(target.getClass(), internalProvider);
                    }
                }
            }
            if (!injectors.isEmpty()) {
                for (Injector injector : injectors) {
                    injector.inject(target, internalProvider, resolutionContext);
                }
            }
            return null;
        }

        @Override public Injector replicateWith(GraphContext context) {
            if (injectors == null) {
                return new LazyCompositeInjector(classWalker, qualifierResolver, resourceMetadata, context);
            }
            List<Injector> newInjectors = new ArrayList<>();
            for (Injector injector : injectors) {
                newInjectors.add(injector.replicateWith(context));
            }
            return new CompositeInjector(newInjectors);
        }

        @Override public List<Dependency> dependencies() {
            if (injectors == null) {
                throw new IllegalStateException("The provision [" + resourceMetadata.toString()
                        + "] cannot have it's dependencies queried before it has been initialized.");
            }
            List<Dependency> dependencies = new LinkedList<>();
            for (Injector injector : injectors) {
                dependencies.addAll(injector.dependencies());
            }
            return dependencies;
        }

    }

    private static class InstantiatorImpl implements Instantiator {

        private final Injector injector;

        InstantiatorImpl(Injector injector) {
            this.injector = injector;
        }

        @Override public List<Dependency> dependencies() {
            return injector.dependencies();
        }

        @Override public Object newInstance(InternalProvider provider, ResolutionContext resolutionContext) {
            return injector.inject(null, provider, resolutionContext);
        }

        @Override public Instantiator replicateWith(GraphContext context) {
            return new InstantiatorImpl(injector.replicateWith(context));
        }

    }

    private static class StatefulInstantiator implements Instantiator {

        private final Injector injector;
        private final ResourceMetadata<?> resourceMetadata;
        private final Dependency moduleDependency;

        StatefulInstantiator(Injector injector,
                             ResourceMetadata<?> resourceMetadata,
                             Dependency moduleDependency) {
            this.injector = injector;
            this.resourceMetadata = resourceMetadata;
            this.moduleDependency = moduleDependency;
        }

        @Override public List<Dependency> dependencies() {
            return injector.dependencies();
        }

        @Override public Object newInstance(InternalProvider provider, ResolutionContext resolutionContext) {
            ProvisionStrategy provisionStrategy = provider
                    .getProvisionStrategy(DependencyRequest.create(resourceMetadata, moduleDependency));
            if (provisionStrategy == null) {
                throw new IllegalStateException(
                        "Missing stateful module defined by " + moduleDependency);
            }
            return injector.inject(
                    provisionStrategy.get(provider, resolutionContext), provider, resolutionContext);
        }

        @Override public Instantiator replicateWith(GraphContext context) {
            return new StatefulInstantiator(
                    injector.replicateWith(context),
                    resourceMetadata,
                    moduleDependency);
        }

    }

    private static class ProvidedModuleInstantiator implements Instantiator {

        private final Class<?> moduleClass;
        private final Object instance;

        ProvidedModuleInstantiator(Class<?> moduleClass) {
            this.moduleClass = moduleClass;
            this.instance = null;
        }

        ProvidedModuleInstantiator(Class<?> moduleClass,
                                   Object instance) {
            this.moduleClass = moduleClass;
            this.instance = instance;
        }

        @Override public List<Dependency> dependencies() {
            return Collections.emptyList();
        }

        @Override public Object newInstance(InternalProvider provider, ResolutionContext resolutionContext) {
            // TODO message
            if (instance == null) {
                throw new IllegalStateException(moduleClass + " is null");
            }
            return instance;
        }

        @Override public Instantiator replicateWith(GraphContext context) {
            return new ProvidedModuleInstantiator(
                    moduleClass,
                    context.statefulSource(moduleClass));
        }

    }

}
