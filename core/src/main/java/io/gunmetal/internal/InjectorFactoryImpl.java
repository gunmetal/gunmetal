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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author rees.byars
 */
class InjectorFactoryImpl implements InjectorFactory {

    private final AnnotationResolver<Qualifier> qualifierResolver;
    private final Linkers linkers;

    InjectorFactoryImpl(AnnotationResolver<Qualifier> qualifierResolver, Linkers linkers) {
        this.qualifierResolver = qualifierResolver;
        this.linkers = linkers;
    }
    
    @Override public <T> StaticInjector<T> staticInjector(final Method method, final ComponentMetadata componentMetadata) {
        ParameterizedFunction function = new MethodFunction(method);
        final Dependency<?>[] dependencies = new Dependency[function.getParameterTypes().length];
        for (int i = 0; i < dependencies.length; i++) {
            dependencies[i] = new Parameter(function, i).asDependency();
        }
        return new StaticInjector<T>() {

            ProvisionStrategy<?>[] provisionStrategies = new ProvisionStrategy[dependencies.length];

            {
                method.setAccessible(true);
                linkers.add(new Linker() {
                    @Override public void link(InternalProvider internalProvider, ResolutionContext linkingContext) {
                        for (int i = 0; i < dependencies.length; i++) {
                            provisionStrategies[i] = internalProvider.getProvisionStrategy(
                                    DependencyRequest.Factory.create(componentMetadata, dependencies[i]));
                        }
                    }
                }, LinkingPhase.POST_WIRING);
            }

            @Override public T inject(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                Object[] parameters = new Object[provisionStrategies.length];
                for (int i = 0; i < parameters.length; i++) {
                    parameters[i] = provisionStrategies[i].get(internalProvider, resolutionContext);
                }
                try {
                    return Smithy.cloak(method.invoke(null, parameters));
                } catch (IllegalAccessException e) {
                    throw Smithy.<RuntimeException>cloak(e);
                } catch (InvocationTargetException e) {
                    throw Smithy.<RuntimeException>cloak(e);
                }
            }

            @Override public Collection<Dependency<?>> dependencies() {
                return Arrays.asList(dependencies);
            }
        };
    }

    @Override public <T> Injector<T> compositeInjector(ComponentMetadata<Class<?>> componentMetadata) {
        return null;
    }

    @Override public <T> Injector<T> lazyCompositeInjector(ComponentMetadata<?> componentMetadata) {
        return null;
    }

    @Override public <T> Instantiator<T> constructorInstantiator(ComponentMetadata<Class<?>> componentMetadata) {
        return null;
    }

    @Override public <T> Instantiator<T> methodInstantiator(final ComponentMetadata<Method> componentMetadata) {
        return new Instantiator<T>() {
            StaticInjector<T> staticInjector = staticInjector(componentMetadata.provider(), componentMetadata);
            @Override public T newInstance(InternalProvider provider, ResolutionContext resolutionContext) {
                return staticInjector.inject(provider, resolutionContext);
            }
            @Override public Collection<Dependency<?>> dependencies() {
                return staticInjector.dependencies();
            }
        };
    }

    private interface ParameterizedFunction {
        Type[] getParameterTypes();
        Annotation[][] getParameterAnnotations();
    }

    private static class MethodFunction implements ParameterizedFunction {

        final Method method;

        MethodFunction(Method method) {
            this.method = method;
        }

        @Override public Type[] getParameterTypes() {
            return method.getGenericParameterTypes();
        }
        @Override public Annotation[][] getParameterAnnotations() {
            return method.getParameterAnnotations();
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
                    return Smithy.cloak(annotation);
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
            return new Dependency<T>() {
                Qualifier qualifier = qualifierResolver.resolve(Parameter.this);
                @Override Qualifier qualifier() {
                    return qualifier;
                }
                @Override TypeKey<T> typeKey() {
                    return Types.typeKey(type);
                }
            };
        }

    }


}
