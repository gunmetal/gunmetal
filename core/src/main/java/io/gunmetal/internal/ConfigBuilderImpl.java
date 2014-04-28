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

import io.gunmetal.AutoCollection;
import io.gunmetal.Inject;
import io.gunmetal.Lazy;
import io.gunmetal.Option;
import io.gunmetal.OverrideEnabled;
import io.gunmetal.Prototype;
import io.gunmetal.Provider;
import io.gunmetal.ProviderDecorator;
import io.gunmetal.spi.AnnotationResolver;
import io.gunmetal.spi.ClassWalker;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.ComponentMetadataResolver;
import io.gunmetal.spi.Config;
import io.gunmetal.spi.ConfigBuilder;
import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.InjectionResolver;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.Scope;
import io.gunmetal.spi.ScopeBindings;
import io.gunmetal.spi.Scopes;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
public class ConfigBuilderImpl implements ConfigBuilder {
    @Override public Config build(Option[] options) {
        return new Config() {

            @Override public ClassWalker classWalker() {
                return new ClassWalkerImpl(new InjectionResolver() {
                    @Override public boolean shouldInject(Field field) {
                        return field.isAnnotationPresent(Inject.class);
                    }
                    @Override public boolean shouldInject(Method method) {
                        return method.isAnnotationPresent(Inject.class);
                    }
                });
            }

            @Override public AnnotationResolver<Qualifier> qualifierResolver() {
                return new AnnotationResolver<Qualifier>() {
                    @Override public Qualifier resolve(AnnotatedElement annotatedElement) {
                        return QualifierBuilder.qualifier(annotatedElement, io.gunmetal.Qualifier.class);
                    }
                };
            }

            @Override public ComponentMetadataResolver componentMetadataResolver() {

                class Resolver {

                    Qualifier qualifier;
                    Scope scope;
                    boolean overrideEnabled = false;
                    boolean collectionElement = false;

                    Resolver(AnnotatedElement annotatedElement) {

                        List<Object> qualifiers = new LinkedList<>();
                        Annotation scopeAnnotation = null;
                        for (Annotation annotation : annotatedElement.getAnnotations()) {
                            Class<? extends Annotation> annotationType = annotation.annotationType();
                            if (annotationType == OverrideEnabled.class) {
                                overrideEnabled = true;
                            } else if (annotationType == AutoCollection.class) {
                                collectionElement = true;
                                qualifiers.add(annotation);
                            }
                            if (annotationType.isAnnotationPresent(io.gunmetal.Scope.class)) {
                                scopeAnnotation = annotation;
                            }
                            if (annotationType.isAnnotationPresent(io.gunmetal.Qualifier.class)
                                    && !qualifiers.contains(annotation)) {
                                qualifiers.add(annotation);
                            }
                        }

                        if (qualifiers.isEmpty()) {
                            qualifier = Qualifier.NONE;
                        } else {
                            qualifier = QualifierBuilder.qualifier(qualifiers.toArray());
                        }
                        if (scopeAnnotation == null) {
                            if (annotatedElement.isAnnotationPresent(Lazy.class)) {
                                scope = Scopes.LAZY_SINGLETON;
                            }
                            scope = Scopes.EAGER_SINGLETON;
                        } else {
                            if (scopeAnnotation instanceof Prototype) {
                                scope = Scopes.PROTOTYPE;
                            } else {
                                throw new UnsupportedOperationException(); // TODO unsupported scope
                            }
                        }

                    }

                }

                return new ComponentMetadataResolver() {

                    @Override public ComponentMetadata<Method> resolveMetadata(final Method method,
                                                                               final ModuleMetadata moduleMetadata) {

                        final Resolver resolver = new Resolver(method);

                        return new ComponentMetadata<Method>() {

                            @Override public Method provider() {
                                return method;
                            }

                            @Override public Class<?> providerClass() {
                                return method.getDeclaringClass();
                            }

                            @Override public ModuleMetadata moduleMetadata() {
                                return moduleMetadata;
                            }

                            @Override public Qualifier qualifier() {
                                return resolver.qualifier;
                            }

                            @Override public Scope scope() {
                                return resolver.scope;
                            }

                            @Override public boolean isOverrideEnabled() {
                                return resolver.overrideEnabled;
                            }

                            @Override public boolean isCollectionElement() {
                                return resolver.collectionElement;
                            }
                        };
                    }

                    @Override public ComponentMetadata<Class<?>> resolveMetadata(final Class<?> cls,
                                                                                 final ModuleMetadata moduleMetadata) {

                        final Resolver resolver = new Resolver(cls);

                        return new ComponentMetadata<Class<?>>() {

                            @Override public Class<?> provider() {
                                return cls;
                            }

                            @Override public Class<?> providerClass() {
                                return cls;
                            }

                            @Override public ModuleMetadata moduleMetadata() {
                                return moduleMetadata;
                            }

                            @Override public Qualifier qualifier() {
                                return resolver.qualifier;
                            }

                            @Override public Scope scope() {
                                return resolver.scope;
                            }

                            @Override public boolean isOverrideEnabled() {
                                return resolver.overrideEnabled;
                            }

                            @Override public boolean isCollectionElement() {
                                return resolver.collectionElement;
                            }
                        };
                    }
                };
            }

            @Override public ConstructorResolver constructorResolver() {
                return new ConstructorResolver() {
                    @Override public <T> Constructor<T> resolve(Class<T> cls) {
                        for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
                            if (constructor.getParameterTypes().length == 0) {
                                return Smithy.cloak(constructor);
                            }
                        }
                        return Smithy.cloak(cls.getDeclaredConstructors()[0]);
                    }
                };
            }

            @Override public ScopeBindings scopeBindings() {
                return new ScopeBindings() {
                    @Override public ProviderDecorator decoratorFor(Scope scope) {
                        if (scope == Scopes.UNDEFINED) {
                            return new ProviderDecorator() {
                                @Override public <T> Provider<T> decorate(Object hashKey, Provider<T> provider) {
                                    return provider;
                                }
                            };
                        }
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override public boolean isProvider(Dependency<?> dependency) {
                return Provider.class.isAssignableFrom(dependency.typeKey().raw());
            }

            @Override public Object provider(Provider<?> provider) {
                return provider;
            }

        };
    }

    private static class QualifierBuilder {

        static Qualifier qualifier(AnnotatedElement annotatedElement,
                                   Class<? extends Annotation> qualifierAnnotation) {
            List<Object> qualifiers = new LinkedList<>();
            for (Annotation annotation : annotatedElement.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (annotationType.isAnnotationPresent(qualifierAnnotation) && !qualifiers.contains(annotation)) {
                    qualifiers.add(annotation);
                } else if (annotationType == AutoCollection.class) {
                    qualifiers.add(annotation);
                }
            }
            if (qualifiers.isEmpty()) {
                return Qualifier.NONE;
            }
            return qualifier(qualifiers.toArray());
        }

        static Qualifier qualifier(final Object[] q) {
            Arrays.sort(q);
            return new Qualifier() {

                Object[] qualifiers = q;
                int hashCode = Arrays.hashCode(qualifiers);

                @Override public Object[] qualifiers() {
                    return qualifiers;
                }

                @Override public Qualifier merge(Qualifier other) {
                    if (qualifiers.length == 0) {
                        return other;
                    }
                    if (other.qualifiers().length == 0) {
                        return this;
                    }
                    List<Object> qualifierList = new LinkedList<>();
                    Collections.addAll(qualifierList, other.qualifiers());
                    for (Object qualifier : qualifiers) {
                        if (!qualifierList.contains(qualifier)) {
                            qualifierList.add(qualifier);
                        }
                    }
                    return qualifier(qualifierList.toArray());
                }

                @Override public boolean intersects(Object[] otherQualifiers) {
                    for (Object qualifier : qualifiers) {
                        for (Object otherQualifier : otherQualifiers) {
                            if (qualifier.equals(otherQualifier)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }

                @Override public boolean intersects(Qualifier qualifier) {
                    return intersects(qualifier.qualifiers());
                }

                @Override public boolean equals(Object o) {
                    return o instanceof Qualifier
                            && (this == o || Arrays.equals(((Qualifier) o).qualifiers(), qualifiers));
                }

                @Override public int hashCode() {
                    return hashCode;
                }

            };

        }

    }
}
