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

import io.gunmetal.spi.AnnotationResolver;
import io.gunmetal.spi.ClassWalker;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResolutionContext;

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

    private final AnnotationResolver<Qualifier> qualifierResolver;
    private final ConstructorResolver constructorResolver;
    private final ClassWalker classWalker;
    private final Linkers linkers;

    InjectorFactoryImpl(AnnotationResolver<Qualifier> qualifierResolver,
                        ConstructorResolver constructorResolver,
                        ClassWalker classWalker,
                        Linkers linkers) {
        this.qualifierResolver = qualifierResolver;
        this.constructorResolver = constructorResolver;
        this.classWalker = classWalker;
        this.linkers = linkers;
    }

    @Override public StaticInjector staticInjector(final Method method, final ComponentMetadata<?> componentMetadata) {
        final ParameterizedFunctionInvoker invoker = eagerInvoker(
                new MethodFunction(method),
                componentMetadata);
        return new StaticInjector() {
            @Override public Object inject(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                return invoker.invoke(null, internalProvider, resolutionContext);
            }
            @Override public List<Dependency<?>> dependencies() {
                return invoker.dependencies();
            }
        };
    }

    @Override public <T> Injector<T> compositeInjector(final ComponentMetadata<Class<?>> componentMetadata) {
        final List<Injector<T>> injectors = new ArrayList<>();
        classWalker.walk(componentMetadata.provider(), new ClassWalker.InjectedMemberVisitor() {
            @Override public void visit(final Field field) {
                final Dependency<?> dependency = Dependency.from(
                        qualifierResolver.resolve(field), field.getGenericType());
                injectors.add(new Injector<T>() {
                    ProvisionStrategy<?> provisionStrategy;
                    {
                        field.setAccessible(true);
                        linkers.add(new Linker() {
                            @Override public void link(InternalProvider internalProvider,
                                                       ResolutionContext linkingContext) {
                                provisionStrategy = internalProvider.getProvisionStrategy(
                                        DependencyRequest.Factory.create(componentMetadata, dependency));
                            }
                        }, LinkingPhase.POST_WIRING);
                    }
                    @Override public Object inject(T target, InternalProvider internalProvider,
                                                   ResolutionContext resolutionContext) {
                        try {
                            field.set(target, provisionStrategy.get(internalProvider, resolutionContext));
                            return null;
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("TODO injection exception", e);
                        }
                    }
                    @Override public List<Dependency<?>> dependencies() {
                        return Collections.<Dependency<?>>singletonList(dependency);
                    }
                });
            }
            @Override public void visit(Method method) {
                final ParameterizedFunctionInvoker invoker = eagerInvoker(
                        new MethodFunction(method),
                        componentMetadata);
                injectors.add(new Injector<T>() {
                    @Override public Object inject(T target, InternalProvider internalProvider,
                                                   ResolutionContext resolutionContext) {
                        return invoker.invoke(target, internalProvider, resolutionContext);
                    }

                    @Override public List<Dependency<?>> dependencies() {
                        return invoker.dependencies();
                    }
                });
            }
        });
        return new Injector<T>() {
            @Override public Object inject(T target, InternalProvider internalProvider,
                                           ResolutionContext resolutionContext) {
                if (!injectors.isEmpty()) {
                    for (Injector<T> injector : injectors) {
                        injector.inject(target, internalProvider, resolutionContext);
                    }
                }
                return null;
            }
            @Override public List<Dependency<?>> dependencies() {
                List<Dependency<?>> dependencies = new LinkedList<>();
                for (Injector<T> injector : injectors) {
                    dependencies.addAll(injector.dependencies());
                }
                return dependencies;
            }
        };
    }

    @Override public <T> Injector<T> lazyCompositeInjector(final ComponentMetadata<?> componentMetadata) {
        return new Injector<T>() {
            volatile List<Injector<T>> injectors;
            void init(Class<?> targetClass,
                      final InternalProvider internalProvider) {
                injectors = new ArrayList<>();
                classWalker.walk(targetClass, new ClassWalker.InjectedMemberVisitor() {
                    @Override public void visit(final Field field) {
                        final Dependency<?> dependency = Dependency.from(
                                qualifierResolver.resolve(field), field.getGenericType());
                        injectors.add(new Injector<T>() {
                            ProvisionStrategy<?> provisionStrategy = internalProvider.getProvisionStrategy(
                                    DependencyRequest.Factory.create(componentMetadata, dependency));
                            @Override public Object inject(T target, InternalProvider internalProvider,
                                                           ResolutionContext resolutionContext) {
                                try {
                                    field.set(target, provisionStrategy.get(internalProvider, resolutionContext));
                                    return null;
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException("TODO injection exception", e);
                                }
                            }
                            @Override public List<Dependency<?>> dependencies() {
                                return Collections.<Dependency<?>>singletonList(dependency);
                            }
                        });
                    }
                    @Override public void visit(Method method) {
                        final ParameterizedFunctionInvoker invoker = lazyInvoker(
                                new MethodFunction(method),
                                componentMetadata,
                                internalProvider);
                        injectors.add(new Injector<T>() {
                            @Override public Object inject(T target, InternalProvider internalProvider,
                                                           ResolutionContext resolutionContext) {
                                return invoker.invoke(target, internalProvider, resolutionContext);
                            }
                            @Override public List<Dependency<?>> dependencies() {
                                return invoker.dependencies();
                            }
                        });
                    }
                });
            }
            @Override public Object inject(T target, InternalProvider internalProvider,
                                           ResolutionContext resolutionContext) {
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
            @Override public List<Dependency<?>> dependencies() {
                if (injectors == null) {
                    throw new IllegalStateException("The component [" + componentMetadata.toString()
                        + "] cannot have it's dependencies queried before it has been initialized.");
                }
                List<Dependency<?>> dependencies = new LinkedList<>();
                for (Injector<T> injector : injectors) {
                    dependencies.addAll(injector.dependencies());
                }
                return dependencies;
            }
        };
    }

    @Override public <T> Instantiator<T> constructorInstantiator(ComponentMetadata<Class<?>> componentMetadata) {
        Constructor<?> constructor = constructorResolver.resolve(componentMetadata.provider());
        final ParameterizedFunctionInvoker invoker = eagerInvoker(
                new ConstructorFunction(constructor),
                componentMetadata);
        return new Instantiator<T>() {
            @Override public T newInstance(InternalProvider provider, ResolutionContext resolutionContext) {
                return Smithy.cloak(invoker.invoke(null, provider, resolutionContext));
            }
            @Override public List<Dependency<?>> dependencies() {
                return invoker.dependencies();
            }
        };
    }

    @Override public <T> Instantiator<T> methodInstantiator(final ComponentMetadata<Method> componentMetadata) {
        final ParameterizedFunctionInvoker invoker = eagerInvoker(
                new MethodFunction(componentMetadata.provider()),
                componentMetadata);
        return new Instantiator<T>() {
            @Override public T newInstance(InternalProvider provider, ResolutionContext resolutionContext) {
                return Smithy.cloak(invoker.invoke(null, provider, resolutionContext));
            }
            @Override public List<Dependency<?>> dependencies() {
                return invoker.dependencies();
            }
        };
    }

    private ParameterizedFunctionInvoker eagerInvoker(final ParameterizedFunction function,
                                                        final ComponentMetadata<?> metadata) {
        final Dependency<?>[] dependencies = new Dependency<?>[function.getParameterTypes().length];
        for (int i = 0; i < dependencies.length; i++) {
            dependencies[i] = new Parameter<>(function, i).asDependency();
        }
        return new ParameterizedFunctionInvoker() {
            final ProvisionStrategy<?>[] provisionStrategies = new ProvisionStrategy[dependencies.length];
            {
                linkers.add(new Linker() {
                    @Override public void link(InternalProvider internalProvider, ResolutionContext linkingContext) {
                        for (int i = 0; i < dependencies.length; i++) {
                            provisionStrategies[i] = internalProvider.getProvisionStrategy(
                                    DependencyRequest.Factory.create(metadata, dependencies[i]));
                        }
                    }
                }, LinkingPhase.POST_WIRING);
            }
            @Override public Object invoke(Object onInstance, InternalProvider internalProvider, ResolutionContext resolutionContext) {
                Object[] parameters = new Object[provisionStrategies.length];
                for (int i = 0; i < parameters.length; i++) {
                    parameters[i] = provisionStrategies[i].get(internalProvider, resolutionContext);
                }
                try {
                    return function.invoke(onInstance, parameters);
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    throw new RuntimeException("TODO injection exception", e);
                }
            }
            @Override public List<Dependency<?>> dependencies() {
                return Arrays.asList(dependencies);
            }
        };
    }

    private ParameterizedFunctionInvoker lazyInvoker(final ParameterizedFunction function,
                                                             final ComponentMetadata<?> metadata,
                                                             final InternalProvider internalProvider) {
        final Dependency<?>[] dependencies = new Dependency<?>[function.getParameterTypes().length];
        for (int i = 0; i < dependencies.length; i++) {
            dependencies[i] = new Parameter<>(function, i).asDependency();
        }
        return new ParameterizedFunctionInvoker() {
            final ProvisionStrategy<?>[] provisionStrategies = new ProvisionStrategy[dependencies.length];
            {
                for (int i = 0; i < dependencies.length; i++) {
                    provisionStrategies[i] = internalProvider.getProvisionStrategy(
                            DependencyRequest.Factory.create(metadata, dependencies[i]));
                }
            }
            @Override public Object invoke(Object onInstance, InternalProvider internalProvider, ResolutionContext resolutionContext) {
                Object[] parameters = new Object[provisionStrategies.length];
                for (int i = 0; i < parameters.length; i++) {
                    parameters[i] = provisionStrategies[i].get(internalProvider, resolutionContext);
                }
                try {
                    return function.invoke(onInstance, parameters);
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    throw new RuntimeException("TODO injection exception", e);
                }
            }
            @Override public List<Dependency<?>> dependencies() {
                return Arrays.asList(dependencies);
            }
        };
    }

    private interface ParameterizedFunctionInvoker extends Dependent {
        Object invoke(Object onInstance, InternalProvider internalProvider, ResolutionContext resolutionContext);
    }

    private interface ParameterizedFunction {
        Object invoke(Object onInstance, Object[] params) throws InvocationTargetException, IllegalAccessException, InstantiationException;
        Type[] getParameterTypes();
        Annotation[][] getParameterAnnotations();
    }

    private static class MethodFunction implements ParameterizedFunction {
        final Method method;
        MethodFunction(Method method) {
            method.setAccessible(true);
            this.method = method;
        }
        @Override public Object invoke(Object onInstance, Object[] params) throws InvocationTargetException, IllegalAccessException {
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
        @Override public Object invoke(Object onInstance, Object[] params) throws InvocationTargetException, IllegalAccessException, InstantiationException {
            return constructor.newInstance(params);
        }
        @Override public Type[] getParameterTypes() {
            return constructor.getGenericParameterTypes();
        }
        @Override public Annotation[][] getParameterAnnotations() {
            return constructor.getParameterAnnotations();
        }
    }

    private class Parameter<T> implements AnnotatedElement {

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

        Dependency<T> asDependency() {
            return Dependency.from(qualifierResolver.resolve(Parameter.this), type);
        }

    }

}
