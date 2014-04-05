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
import io.gunmetal.OverrideEnabled;
import io.gunmetal.Provider;
import io.gunmetal.spi.AnnotationResolver;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.Config;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.spi.Scope;
import io.gunmetal.spi.Scopes;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author rees.byars
 */
public class ApplicationBuilderImpl implements ApplicationBuilder {

    @Override public ApplicationContainer build(Class<?> application) {

        ApplicationModule applicationModule = application.getAnnotation(ApplicationModule.class);

        final Config config = new ConfigBuilderImpl().build(applicationModule.options());

        ApplicationLinker applicationLinker = new ApplicationLinker();

        InjectorFactory injectorFactory = new InjectorFactoryImpl(
                config.qualifierResolver(),
                config.constructorResolver(),
                config.classWalker(),
                applicationLinker);

        final List<ProvisionStrategyDecorator> strategyDecorators = new ArrayList<>();
        strategyDecorators.add(new ScopeDecorator(config.scopeBindings(), applicationLinker));
        ProvisionStrategyDecorator compositeStrategyDecorator = new ProvisionStrategyDecorator() {
            @Override public <T> ProvisionStrategy<T> decorate(
                    ComponentMetadata<?> componentMetadata, ProvisionStrategy<T> delegateStrategy) {
                for (ProvisionStrategyDecorator decorator : strategyDecorators) {
                    delegateStrategy = decorator.decorate(componentMetadata, delegateStrategy);
                }
                return delegateStrategy;
            }
        };

        final ComponentAdapterFactory componentAdapterFactory =
                new ComponentAdapterFactoryImpl(injectorFactory, compositeStrategyDecorator);

        final HandlerFactory handlerFactory = new HandlerFactoryImpl(
                componentAdapterFactory,
                config.qualifierResolver(),
                config.scopeResolver(),
                // TODO move to config
                new AnnotationResolver<Boolean>() {
                    @Override public Boolean resolve(AnnotatedElement annotatedElement) {
                        return annotatedElement.isAnnotationPresent(OverrideEnabled.class);
                    }
                },
                new AnnotationResolver<Dependency.Kind>() {
                    @Override public Dependency.Kind resolve(AnnotatedElement annotatedElement) {
                        throw new UnsupportedOperationException();
                    }
                });

        final HandlerCache handlerCache = new HandlerCache();

        for (Class<?> module : applicationModule.modules()) {
            List<DependencyRequestHandler<?>> moduleRequestHandlers =
                    handlerFactory.createHandlersForModule(module);
            handlerCache.putAll(moduleRequestHandlers);
        }

        final InternalProvider internalProvider = new InternalProvider() {
            @Override public <T> ProvisionStrategy<? extends T> getProvisionStrategy(final DependencyRequest<T> dependencyRequest) {
                final Dependency<T> dependency = dependencyRequest.dependency();
                DependencyRequestHandler<? extends T> requestHandler = handlerCache.get(dependency);
                if (requestHandler != null) {
                    return requestHandler
                            .handle(dependencyRequest)
                            .validateResponse()
                            .getProvisionStrategy();
                }
                if (config.isProvider(dependency)) {
                    requestHandler = createProviderHandler(dependencyRequest, config, this, handlerCache);
                    if (requestHandler != null) {
                        handlerCache.put(dependency, requestHandler);
                        return requestHandler
                                .handle(dependencyRequest)
                                .validateResponse()
                                .getProvisionStrategy();
                    }
                }
                requestHandler = handlerFactory.attemptToCreateHandlerFor(dependencyRequest);
                if (requestHandler != null) {
                    // we do not cache handlers for specific requests - each request gets its own
                    return requestHandler
                            .handle(dependencyRequest)
                            .validateResponse()
                            .getProvisionStrategy();
                }
                throw new DependencyException("missing dependency " + dependency.toString()); // TODO
            }
        };

        applicationLinker.link(internalProvider, ResolutionContext.Factory.create());

        return new ApplicationContainer() {

            final Map<Class<?>, Injector<?>> injectors = new HashMap<>();
            final InjectorFactory injectorFactory = new InjectorFactoryImpl(
                    config.qualifierResolver(),
                    config.constructorResolver(),
                    config.classWalker(),
                    new Linkers() {
                        @Override public void add(Linker linker, LinkingPhase phase) {
                            linker.link(internalProvider, ResolutionContext.Factory.create());
                        }
                    });

            @Override public <T> ApplicationContainer inject(T injectionTarget) {

                final Class<T> targetClass = Smithy.cloak(injectionTarget.getClass());

                Injector<T> injector = Smithy.cloak(injectors.get(targetClass));

                if (injector == null) {
                    final Qualifier qualifier = config.qualifierResolver().resolve(targetClass);
                    injector = injectorFactory.compositeInjector(new ComponentMetadata<Class<?>>() {
                        @Override public Class<?> provider() {
                            return targetClass;
                        }

                        @Override public Class<?> providerClass() {
                            return targetClass;
                        }

                        @Override public ModuleMetadata moduleMetadata() {
                            return new ModuleMetadata() {
                                @Override public Class<?> moduleClass() {
                                    return targetClass;
                                }

                                @Override public Qualifier qualifier() {
                                    return qualifier;
                                }

                                @Override public Class<?>[] referencedModules() {
                                    return new Class<?>[0];
                                }
                            };
                        }

                        @Override public Qualifier qualifier() {
                            return qualifier;
                        }

                        @Override public Scope scope() {
                            return Scopes.UNDEFINED;
                        }
                    });
                    injectors.put(targetClass, injector);
                }

                injector.inject(injectionTarget, internalProvider, ResolutionContext.Factory.create());

                return this;
            }

            @Override public <T, D extends io.gunmetal.Dependency<T>> T get(Class<D> dependencySpec) {
                Qualifier qualifier = config.qualifierResolver().resolve(dependencySpec);
                Type parameterizedDependencySpec = dependencySpec.getGenericInterfaces()[0];
                Type dependencyType = ((ParameterizedType) parameterizedDependencySpec).getActualTypeArguments()[0];
                Dependency<T> dependency = Dependency.from(
                        qualifier,
                        dependencyType);
                DependencyRequestHandler<? extends T> requestHandler = handlerCache.get(dependency);
                if (requestHandler != null) {
                    return requestHandler.force().get(internalProvider, ResolutionContext.Factory.create());
                } else if (config.isProvider(dependency)) {
                    ProvisionStrategy<T> providerStrategy =
                            createProviderStrategy(dependency, config, internalProvider, handlerCache);
                    if (providerStrategy != null) {
                        return providerStrategy.get(internalProvider, ResolutionContext.Factory.create());
                    }
                }
                return null;
            }
        };
    }

    private <T> ProvisionStrategy<T> createProviderStrategy(final ProvisionStrategy<?> componentStrategy,
                                                            final Config config,
                                                            final InternalProvider internalProvider) {
        final Object provider = config.provider(new Provider<Object>() {

            // TODO this is a hack for issue #2 :/
            final ThreadLocal<ResolutionContext> contextThreadLocal = new ThreadLocal<>();

            @Override public Object get() {

                ResolutionContext context = contextThreadLocal.get();

                if (context != null) {
                    return componentStrategy.get(
                            internalProvider, context);
                }

                try {
                    context = ResolutionContext.Factory.create();
                    contextThreadLocal.set(context);
                    return componentStrategy.get(
                            internalProvider, context);
                } finally {
                    contextThreadLocal.remove();
                }

            }

        });
        return new ProvisionStrategy<T>() {
            @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                return Smithy.cloak(provider);
            }
        };
    }

    private <T, C> ProvisionStrategy<T> createProviderStrategy(
            final Dependency<T> providerDependency,
            final Config config,
            final InternalProvider internalProvider,
            final HandlerCache handlerCache) {
        Type providedType = ((ParameterizedType) providerDependency.typeKey().type()).getActualTypeArguments()[0];
        final Dependency<C> componentDependency = Dependency.from(providerDependency.qualifier(), providedType);
        final DependencyRequestHandler<? extends C> componentHandler = handlerCache.get(componentDependency);
        if (componentHandler == null) {
            return null;
        }
        ProvisionStrategy<?> componentStrategy = componentHandler.force();
        return createProviderStrategy(componentStrategy, config, internalProvider);
    }

    private <T, C> DependencyRequestHandler<T> createProviderHandler(
            final DependencyRequest<T> providerRequest,
            final Config config,
            final InternalProvider internalProvider,
            final HandlerCache handlerCache) {
        Dependency<T> providerDependency = providerRequest.dependency();
        Type providedType = ((ParameterizedType) providerDependency.typeKey().type()).getActualTypeArguments()[0];
        final Dependency<C> componentDependency = Dependency.from(providerDependency.qualifier(), providedType);
        final DependencyRequestHandler<? extends C> componentHandler = handlerCache.get(componentDependency);
        if (componentHandler == null) {
            return null;
        }
        ProvisionStrategy<? extends C> componentStrategy = componentHandler.force();
        final ProvisionStrategy<T> providerStrategy =
                createProviderStrategy(componentStrategy, config, internalProvider);
        return new DependencyRequestHandler<T>() {
            @Override public List<Dependency<? super T>> targets() {
                return Collections.<Dependency<? super T>>singletonList(providerRequest.dependency());
            }

            @Override public List<Dependency<?>> dependencies() {
                return componentHandler.dependencies();
            }

            @Override public DependencyResponse<T> handle(DependencyRequest<? super T> dependencyRequest) {
                final DependencyResponse<?> componentResponse =
                        componentHandler.handle(DependencyRequest.Factory.create(providerRequest, componentDependency));
                return new DependencyResponse<T>() {
                    @Override public ValidatedDependencyResponse<T> validateResponse() {
                        componentResponse.validateResponse();
                        return new ValidatedDependencyResponse<T>() {
                            @Override public ProvisionStrategy<T> getProvisionStrategy() {
                                return providerStrategy;
                            }

                            @Override public ValidatedDependencyResponse<T> validateResponse() {
                                return this;
                            }
                        };
                    }
                };
            }

            @Override public ProvisionStrategy<T> force() {
                return providerStrategy;
            }

            @Override public boolean isOverrideEnabled() {
                return componentHandler.isOverrideEnabled();
            }
        };

    }

    private static class HandlerCache {

        final Map<Dependency<?>, DependencyRequestHandler<?>> requestHandlers = new HashMap<>();

        synchronized void putAll(List<DependencyRequestHandler<?>> requestHandlers) {
            for (DependencyRequestHandler<?> requestHandler : requestHandlers) {
                putAll(requestHandler);
            }
        }

        synchronized <T> void putAll(DependencyRequestHandler<T> requestHandler) {
            for (Dependency<? super T> dependency : requestHandler.targets()) {
                put(dependency, requestHandler);
            }
        }

        synchronized <T> void put(Dependency<? super T> dependency, DependencyRequestHandler<T> requestHandler) {
            DependencyRequestHandler<?> previous = requestHandlers.get(dependency);
            if (previous != null) {
                // TODO better messages.
                // TODO This could randomly pass/fail in case of multiple non-override enabled
                // TODO handlers with a single enabled handler.  Low priority.
                if (previous.isOverrideEnabled() && requestHandler.isOverrideEnabled()) {
                    throw new RuntimeException("more than one of type with override enabled");
                } else if (!previous.isOverrideEnabled() && !requestHandler.isOverrideEnabled()) {
                    throw new RuntimeException("more than one of type without override enabled");
                } else if (requestHandler.isOverrideEnabled()) {
                    requestHandlers.put(dependency, requestHandler);
                } // else keep previous
            } else {
                requestHandlers.put(dependency, requestHandler);
            }
        }

        <T> DependencyRequestHandler<? extends T> get(Dependency<T> dependency) {
            return Smithy.cloak(requestHandlers.get(dependency));
        }

    }

    private static class ApplicationLinker implements Linkers, Linker {

        final Stack<Linker> postWiringLinkers = new Stack<>();
        final Stack<Linker> eagerLinkers = new Stack<>();

        @Override public void add(Linker linker, LinkingPhase phase) {
            switch (phase) {
                case POST_WIRING: postWiringLinkers.push(linker); break;
                case EAGER_INSTANTIATION: eagerLinkers.add(linker); break;
                default: throw new UnsupportedOperationException("Phase unsupported:  " + phase);
            }
        }

        @Override public void link(InternalProvider internalProvider, ResolutionContext linkingContext) {
            while (!postWiringLinkers.empty()) {
                postWiringLinkers.pop().link(internalProvider, linkingContext);
            }
            while (!eagerLinkers.empty()) {
                eagerLinkers.pop().link(internalProvider, linkingContext);
            }
        }

    }

}
