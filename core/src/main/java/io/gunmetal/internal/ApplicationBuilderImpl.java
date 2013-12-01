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

import io.gunmetal.ApplicationContainer;
import io.gunmetal.ApplicationModule;
import io.gunmetal.Inject;
import io.gunmetal.Lazy;
import io.gunmetal.Prototype;
import io.gunmetal.ProviderDecorator;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author rees.byars
 */
public class ApplicationBuilderImpl implements ApplicationBuilder {

    @Override public ApplicationContainer build(Class<?> application) {

        ApplicationModule applicationModule = application.getAnnotation(ApplicationModule.class);

        final Config config = new Config();

        final AnnotationResolver<Qualifier> qualifierResolver = new AnnotationResolver<Qualifier>() {
            @Override public Qualifier resolve(AnnotatedElement annotatedElement) {
                return Smithy.qualifier(annotatedElement, config.qualifierAnnotation());
            }
        };

        ConstructorResolver constructorResolver = new ConstructorResolver() {
            @Override public <T> Constructor<T> resolve(Class<T> cls) {
                for (Constructor<?> candidate : cls.getDeclaredConstructors()) {
                    if (config.valid(candidate)) {
                        return Smithy.cloak(candidate);
                    }
                }
                throw new IllegalArgumentException("Could not find a suitable constructor for the class ["
                        + cls.getName() + "]");
            }
        };

        ClassWalker classWalker = new ClassWalkerImpl(config);

        final List<Linker> postWiringLinkers = new LinkedList<Linker>();
        final List<Linker> eagerLinkers = new LinkedList<Linker>();
        Linkers linkers = new Linkers() {
            @Override public void add(Linker linker, LinkingPhase phase) {
                switch (phase) {
                    case POST_WIRING: postWiringLinkers.add(linker);
                    case EAGER_INSTANTIATION: eagerLinkers.add(linker);
                }

            }
        };

        InjectorFactory injectorFactory = new InjectorFactoryImpl(
                qualifierResolver,
                constructorResolver,
                classWalker,
                linkers);

        final List<ProvisionStrategyDecorator> strategyDecorators = new ArrayList<ProvisionStrategyDecorator>();
        strategyDecorators.add(new ScopeDecorator(config, linkers));
        ProvisionStrategyDecorator compositeStrategyDecorator = new ProvisionStrategyDecorator() {
            @Override public <T> ProvisionStrategy<T> decorate(
                    ComponentMetadata<?> componentMetadata, ProvisionStrategy<T> delegateStrategy) {
                for (ProvisionStrategyDecorator decorator : strategyDecorators) {
                    delegateStrategy = decorator.decorate(componentMetadata, delegateStrategy);
                }
                return delegateStrategy;
            }
        };

        ComponentAdapterFactory componentAdapterFactory =
                new ComponentAdapterFactoryImpl(injectorFactory, compositeStrategyDecorator);

        AnnotationResolver<Scope> scopeResolver = new AnnotationResolver<Scope>() {
            @Override public Scope resolve(AnnotatedElement annotatedElement) {
                return config.resolveScope(annotatedElement);
            }
        };

        ModuleParser moduleParser = new ModuleParserImpl(componentAdapterFactory, qualifierResolver, scopeResolver);

        final Map<Dependency<?>, ComponentAdapterProvider<?>> componentAdapterProviders
                = new HashMap<Dependency<?>, ComponentAdapterProvider<?>>();

        for (Class<?> module : applicationModule.modules()) {

            List<ComponentAdapterProvider<?>> moduleAdapterProviders = moduleParser.parse(module);

            for (ComponentAdapterProvider<?> adapterProvider : moduleAdapterProviders) {
                ComponentAdapter<?> adapter = adapterProvider.get();
                final ComponentMetadata<?> metadata = adapter.metadata();
                for (final TypeKey<?> typeKey : metadata.targets()) {
                    componentAdapterProviders.put(new Dependency<Object>() {
                        @Override Qualifier qualifier() {
                            return metadata.qualifier();
                        }
                        @Override TypeKey<Object> typeKey() {
                            return Smithy.cloak(typeKey);
                        }
                    }, adapterProvider);
                }
            }
        }

        final InternalProvider internalProvider = new InternalProvider() {
            @Override public <T> ProvisionStrategy<T> getProvisionStrategy(DependencyRequest dependencyRequest) {
                ComponentAdapterProvider<T> adapterProvider =
                        Smithy.cloak(componentAdapterProviders.get(dependencyRequest.dependency()));

                if (!adapterProvider.isAccessibleTo(dependencyRequest)) {
                    throw new IllegalAccessError(); // TODO
                }
                return adapterProvider.get().provisionStrategy();
            }
        };

        ResolutionContext linkingContext = ResolutionContext.Factory.create();
        for (Linker linker : postWiringLinkers) {
            linker.link(internalProvider, linkingContext);
        }

        ResolutionContext eagerContext = ResolutionContext.Factory.create();
        for (Linker linker : eagerLinkers) {
            linker.link(internalProvider, eagerContext);
        }

        return new ApplicationContainer() {
            @Override public ApplicationContainer inject(Object injectionTarget) {
                throw new UnsupportedOperationException();
            }

            @Override public <T, D extends io.gunmetal.Dependency<T>> T get(Class<D> dependency) {
                final Qualifier qualifier = qualifierResolver.resolve(dependency);
                ParameterizedType parameterizedType = (ParameterizedType) dependency.getGenericInterfaces()[0];
                Type type = parameterizedType.getActualTypeArguments()[0];
                final TypeKey<T> typeKey = Smithy.cloak(Types.typeKey(type));
                ComponentAdapterProvider<T> adapterProvider = Smithy.cloak(
                        componentAdapterProviders.get(new Dependency<T>() {
                    @Override Qualifier qualifier() {
                        return qualifier;
                    }
                    @Override TypeKey<T> typeKey() {
                        return typeKey;
                    }
                }));
                return adapterProvider.get()
                        .provisionStrategy().get(internalProvider, ResolutionContext.Factory.create());
            }
        };
    }

    private static class Config implements InjectionResolver, ScopeBindings {
        Class<? extends Annotation> qualifierAnnotation() {
            return io.gunmetal.Qualifier.class;
        }
        boolean valid(Constructor constructor) {
            return true;
        }
        @Override public boolean shouldInject(Field field) {
            return field.isAnnotationPresent(Inject.class);
        }
        @Override public boolean shouldInject(Method method) {
            return method.isAnnotationPresent(Inject.class);
        }
        @Override public ProviderDecorator decoratorFor(Scope scope) {
            throw new UnsupportedOperationException();
        }
        Scope resolveScope(AnnotatedElement annotatedElement) {
            Annotation scope = Smithy.scope(annotatedElement, io.gunmetal.Scope.class);
            if (scope == null) {
                if (annotatedElement.isAnnotationPresent(Lazy.class)) {
                    return Scopes.LAZY_SINGLETON;
                }
                return Scopes.EAGER_SINGLETON;
            }
            if (scope instanceof Prototype) {
                return Scopes.PROTOTYPE;
            }
            throw new UnsupportedOperationException();
        }
    }

}
