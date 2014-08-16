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
import io.gunmetal.spi.ProvisionMetadata;
import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.QualifierResolver;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.util.Generics;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    @Override public <T> Injector<T> compositeInjector(final ProvisionMetadata<Class<?>> provisionMetadata,
                                                       final GraphContext context) {

        final List<Injector<T>> injectors = new ArrayList<>();

        classWalker.walk(provisionMetadata.provider(),
                field -> {
                    Dependency<?> dependency = Dependency.from(
                            qualifierResolver.resolveDependencyQualifier(
                                    field,
                                    provisionMetadata.moduleMetadata().qualifier(),
                                    (error) -> context.errors().add(provisionMetadata, error)),
                            field.getGenericType());
                    injectors.add(new FieldInjector<>(field, provisionMetadata, dependency, context.linkers()));
                },
                method -> {
                    ParameterizedFunction function = new MethodFunction(method);
                    injectors.add(new FunctionInjector<>(
                            function,
                            provisionMetadata,
                            dependenciesForFunction(
                                    provisionMetadata,
                                    function,
                                    qualifierResolver,
                                    context),
                            context.linkers()));
                },
                provisionMetadata,
                (error) -> context.errors().add(provisionMetadata, error));

        return new CompositeInjector<>(injectors);

    }

    @Override public <T> Injector<T> lazyCompositeInjector(final ProvisionMetadata<?> provisionMetadata,
                                                           GraphContext context) {
        return new LazyCompositeInjector<>(classWalker, qualifierResolver, provisionMetadata, context);
    }

    @Override public <T> Instantiator<T> constructorInstantiator(ProvisionMetadata<Class<?>> provisionMetadata,
                                                                 GraphContext context) {
        Constructor<?> constructor = constructorResolver.resolve(provisionMetadata.provider());
        ParameterizedFunction function = new ConstructorFunction(constructor);
        Injector<?> injector = new FunctionInjector<>(
                function,
                provisionMetadata,
                dependenciesForFunction(
                        provisionMetadata,
                        function,
                        qualifierResolver,
                        context),
                context.linkers());
        return new InstantiatorImpl<>(injector);
    }

    @Override public <T> Instantiator<T> methodInstantiator(ProvisionMetadata<Method> provisionMetadata,
                                                            GraphContext context) {
        ParameterizedFunction function = new MethodFunction(provisionMetadata.provider());
        Injector<?> injector = new FunctionInjector<>(
                function,
                provisionMetadata,
                dependenciesForFunction(
                        provisionMetadata,
                        function,
                        qualifierResolver,
                        context),
                context.linkers());
        return new InstantiatorImpl<>(injector);
    }

    @Override public <T> Instantiator<T> instanceInstantiator(ProvisionMetadata<Class<?>> provisionMetadata,
                                                              GraphContext context) {
        return new ProvidedModuleInstantiator<>(Generics.as(provisionMetadata.providerClass()));
    }

    @Override public <T, S> Instantiator<T> statefulMethodInstantiator(
            ProvisionMetadata<Method> provisionMetadata, Dependency<S> moduleDependency, GraphContext context) {
        ParameterizedFunction function = new MethodFunction(provisionMetadata.provider());
        FunctionInjector<S> injector = new FunctionInjector<>(
                function,
                provisionMetadata,
                dependenciesForFunction(
                        provisionMetadata,
                        function,
                        qualifierResolver,
                        context),
                context.linkers());
        return new StatefulInstantiator<>(injector, provisionMetadata, moduleDependency);
    }

    private static Dependency<?>[] dependenciesForFunction(ProvisionMetadata<?> provisionMetadata,
                                                           ParameterizedFunction function,
                                                           QualifierResolver qualifierResolver,
                                                           GraphContext context) {
        Dependency<?>[] dependencies = new Dependency<?>[function.getParameterTypes().length];
        for (int i = 0; i < dependencies.length; i++) {
            Parameter parameter = new Parameter(function, i);
            dependencies[i] = Dependency.from(
                    qualifierResolver.resolveDependencyQualifier(
                            parameter,
                            provisionMetadata.moduleMetadata().qualifier(),
                            (error) -> context.errors().add(provisionMetadata, error)),
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

        @Override public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            for (Annotation annotation : annotations) {
                if (annotationClass.isInstance(annotation)) {
                    return true;
                }
            }
            return false;
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

    private static class FieldInjector<T> implements Injector<T> {

        private final Field field;
        private final ProvisionMetadata<?> provisionMetadata;
        private final Dependency<?> dependency;
        private ProvisionStrategy<?> provisionStrategy;

        FieldInjector(Field field,
                      final ProvisionMetadata<?> provisionMetadata,
                      final Dependency<?> dependency,
                      Linkers linkers) {
            field.setAccessible(true);
            linkers.addWiringLinker((internalProvider, linkingContext) -> {
                provisionStrategy = internalProvider.getProvisionStrategy(
                        DependencyRequest.create(provisionMetadata, dependency));
            });
            this.field = field;
            this.provisionMetadata = provisionMetadata;
            this.dependency = dependency;
        }

        FieldInjector(Field field,
                      ProvisionMetadata<?> provisionMetadata,
                      Dependency<?> dependency,
                      ProvisionStrategy<?> provisionStrategy) {
            field.setAccessible(true);
            this.field = field;
            this.provisionMetadata = provisionMetadata;
            this.dependency = dependency;
            this.provisionStrategy = provisionStrategy;
        }

        @Override public Object inject(T target,
                                       InternalProvider internalProvider,
                                       ResolutionContext resolutionContext) {
            try {
                field.set(target, provisionStrategy.get(internalProvider, resolutionContext));
                return null;
            } catch (IllegalAccessException e) {
                throw new RuntimeException("An unexpected exception has occurred", e);
            }
        }

        @Override public Injector<T> replicateWith(GraphContext context) {
            return new FieldInjector<>(field, provisionMetadata, dependency, context.linkers());
        }

        @Override public List<Dependency<?>> dependencies() {
            return Collections.<Dependency<?>>singletonList(dependency);
        }

    }

    private static class FunctionInjector<T> implements Injector<T> {

        private final ParameterizedFunction function;
        private final ProvisionMetadata<?> provisionMetadata;
        private final Dependency<?>[] dependencies;
        private final ProvisionStrategy<?>[] provisionStrategies;

        FunctionInjector(final ParameterizedFunction function,
                         final ProvisionMetadata<?> provisionMetadata,
                         final Dependency<?>[] dependencies,
                         final Linkers linkers) {
            this.function = function;
            this.provisionMetadata = provisionMetadata;
            this.dependencies = dependencies;
            provisionStrategies = new ProvisionStrategy<?>[dependencies.length];
            linkers.addWiringLinker((internalProvider, linkingContext) -> {
                for (int i = 0; i < dependencies.length; i++) {
                    provisionStrategies[i] = internalProvider.getProvisionStrategy(
                            DependencyRequest.create(provisionMetadata, dependencies[i]));
                }
            });
        }

        FunctionInjector(ParameterizedFunction function,
                         ProvisionMetadata<?> provisionMetadata,
                         Dependency<?>[] dependencies,
                         ProvisionStrategy<?>[] provisionStrategies) {
            this.function = function;
            this.provisionMetadata = provisionMetadata;
            this.dependencies = dependencies;
            this.provisionStrategies = provisionStrategies;
        }

        @Override public Object inject(T target,
                                       InternalProvider internalProvider,
                                       ResolutionContext resolutionContext) {
            Object[] parameters = new Object[provisionStrategies.length];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = provisionStrategies[i].get(internalProvider, resolutionContext);
            }
            try {
                return function.invoke(target, parameters);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                if (e.getCause() != null && e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        }

        @Override public Injector<T> replicateWith(GraphContext context) {
            return new FunctionInjector<>(function, provisionMetadata, dependencies, context.linkers());
        }

        @Override public List<Dependency<?>> dependencies() {
            return Arrays.asList(dependencies);
        }

    }

    private static class CompositeInjector<T> implements Injector<T> {

        private final List<Injector<T>> injectors;

        CompositeInjector(List<Injector<T>> injectors) {
            this.injectors = injectors;
        }

        @Override public Object inject(T target,
                                       InternalProvider internalProvider,
                                       ResolutionContext resolutionContext) {
            if (!injectors.isEmpty()) {
                for (Injector<T> injector : injectors) {
                    injector.inject(target, internalProvider, resolutionContext);
                }
            }
            return null;
        }

        @Override public Injector<T> replicateWith(GraphContext context) {
            List<Injector<T>> newInjectors = new ArrayList<>();
            for (Injector<T> injector : injectors) {
                newInjectors.add(injector.replicateWith(context));
            }
            return new CompositeInjector<>(newInjectors);
        }

        @Override public List<Dependency<?>> dependencies() {
            List<Dependency<?>> dependencies = new LinkedList<>();
            for (Injector<T> injector : injectors) {
                dependencies.addAll(injector.dependencies());
            }
            return dependencies;
        }

    }

    private static class LazyCompositeInjector<T> implements Injector<T> {

        private final ClassWalker classWalker;
        private final QualifierResolver qualifierResolver;
        private final ProvisionMetadata<?> provisionMetadata;
        private final GraphContext context;
        private volatile List<Injector<T>> injectors;

        LazyCompositeInjector(ClassWalker classWalker,
                              QualifierResolver qualifierResolver,
                              ProvisionMetadata<?> provisionMetadata,
                              GraphContext context) {
            this.classWalker = classWalker;
            this.qualifierResolver = qualifierResolver;
            this.provisionMetadata = provisionMetadata;
            this.context = context;
        }

        void init(Class<?> targetClass, final InternalProvider internalProvider) {

            injectors = new ArrayList<>();

            classWalker.walk(targetClass,
                    field -> {
                        Dependency<?> dependency = Dependency.from(
                                qualifierResolver.resolveDependencyQualifier(
                                        field,
                                        provisionMetadata.moduleMetadata().qualifier(),
                                        (error) -> context.errors().add(provisionMetadata, error)),
                                field.getGenericType());
                        ProvisionStrategy<?> provisionStrategy = internalProvider.getProvisionStrategy(
                                DependencyRequest.create(provisionMetadata, dependency));
                        injectors.add(new FieldInjector<>(field, provisionMetadata, dependency, provisionStrategy));
                    },
                    method -> {
                        ParameterizedFunction function = new MethodFunction(method);
                        Dependency<?>[] dependencies =
                                dependenciesForFunction(
                                        provisionMetadata,
                                        function,
                                        qualifierResolver,
                                        context);
                        ProvisionStrategy<?>[] provisionStrategies = new ProvisionStrategy<?>[dependencies.length];
                        for (int i = 0; i < dependencies.length; i++) {
                            provisionStrategies[i] = internalProvider.getProvisionStrategy(
                                    DependencyRequest.create(provisionMetadata, dependencies[i]));
                        }
                        injectors.add(new FunctionInjector<>(
                                function,
                                provisionMetadata,
                                dependencies,
                                provisionStrategies));
                    },
                    provisionMetadata,
                    (error) -> context.errors().add(provisionMetadata, error));
        }

        @Override public Object inject(
                T target, InternalProvider internalProvider, ResolutionContext resolutionContext) {
            if (injectors == null) {
                synchronized (this) {
                    if (injectors == null) {
                        init(target.getClass(), internalProvider);
                    }
                }
            }
            if (!injectors.isEmpty()) {
                for (Injector<T> injector : injectors) {
                    injector.inject(target, internalProvider, resolutionContext);
                }
            }
            return null;
        }

        @Override public Injector<T> replicateWith(GraphContext context) {
            if (injectors == null) {
                return new LazyCompositeInjector<>(classWalker, qualifierResolver, provisionMetadata, context);
            }
            List<Injector<T>> newInjectors = new ArrayList<>();
            for (Injector<T> injector : injectors) {
                newInjectors.add(injector.replicateWith(context));
            }
            return new CompositeInjector<>(newInjectors);
        }

        @Override public List<Dependency<?>> dependencies() {
            if (injectors == null) {
                throw new IllegalStateException("The provision [" + provisionMetadata.toString()
                        + "] cannot have it's dependencies queried before it has been initialized.");
            }
            List<Dependency<?>> dependencies = new LinkedList<>();
            for (Injector<T> injector : injectors) {
                dependencies.addAll(injector.dependencies());
            }
            return dependencies;
        }

    }

    private static class InstantiatorImpl<T, S> implements Instantiator<T> {

        private final Injector<S> injector;

        InstantiatorImpl(Injector<S> injector) {
            this.injector = injector;
        }

        @Override public List<Dependency<?>> dependencies() {
            return injector.dependencies();
        }

        @SuppressWarnings("unchecked")
        @Override public T newInstance(InternalProvider provider, ResolutionContext resolutionContext) {
            return (T) injector.inject(null, provider, resolutionContext);
        }

        @Override public Instantiator<T> replicateWith(GraphContext context) {
            return new InstantiatorImpl<>(injector.replicateWith(context));
        }

    }

    private static class StatefulInstantiator<T, S> implements Instantiator<T> {

        private final Injector<S> injector;
        private final ProvisionMetadata<Method> provisionMetadata;
        private final Dependency<S> moduleDependency;

        StatefulInstantiator(Injector<S> injector,
                             ProvisionMetadata<Method> provisionMetadata,
                             Dependency<S> moduleDependency) {
            this.injector = injector;
            this.provisionMetadata = provisionMetadata;
            this.moduleDependency = moduleDependency;
        }

        @Override public List<Dependency<?>> dependencies() {
            return injector.dependencies();
        }

        @SuppressWarnings("unchecked")
        @Override public T newInstance(InternalProvider provider, ResolutionContext resolutionContext) {
            ProvisionStrategy<? extends S> provisionStrategy = provider
                    .getProvisionStrategy(DependencyRequest.create(provisionMetadata, moduleDependency));
            if (provisionStrategy == null) {
                throw new IllegalStateException(
                        "Missing stateful module defined by " + moduleDependency);
            }
            return (T) injector.inject(
                    provisionStrategy.get(provider, resolutionContext), provider, resolutionContext);
        }

        @Override public Instantiator<T> replicateWith(GraphContext context) {
            return new StatefulInstantiator<>(
                    injector.replicateWith(context),
                    provisionMetadata,
                    moduleDependency);
        }

    }

    private static class ProvidedModuleInstantiator<T> implements Instantiator<T> {

        private final Class<T> moduleClass;
        private final T instance;

        ProvidedModuleInstantiator(Class<T> moduleClass) {
            this.moduleClass = moduleClass;
            this.instance = null;
        }

        ProvidedModuleInstantiator(Class<T> moduleClass,
                                   T instance) {
            this.moduleClass = moduleClass;
            this.instance = instance;
        }

        @Override public List<Dependency<?>> dependencies() {
            return Collections.emptyList();
        }

        @Override public T newInstance(InternalProvider provider, ResolutionContext resolutionContext) {
            // TODO message
            if (instance == null) {
                throw new IllegalStateException(moduleClass + " is null");
            }
            return instance;
        }

        @Override public Instantiator<T> replicateWith(GraphContext context) {
            return new ProvidedModuleInstantiator<>(
                    moduleClass,
                    context.statefulSource(moduleClass));
        }

    }

}
