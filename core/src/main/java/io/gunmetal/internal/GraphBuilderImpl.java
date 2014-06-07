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

import io.gunmetal.ObjectGraph;
import io.gunmetal.Provider;
import io.gunmetal.RootModule;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.Config;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.Linker;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.util.Generics;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rees.byars
 */
public class GraphBuilderImpl implements GraphBuilder {

    @Override public ObjectGraph build(Class<?> root) {
        return build(root, null);
    }

    private ObjectGraph build(Class<?> root, final HandlerCache parentCache) {

        RootModule rootModule = root.getAnnotation(RootModule.class);

        final Config config = new ConfigBuilderImpl().build(rootModule.options());

        InjectorFactory injectorFactory = new InjectorFactoryImpl(
                config.qualifierResolver(),
                config.constructorResolver(),
                config.classWalker());

        final List<ProvisionStrategyDecorator> strategyDecorators = new ArrayList<>();
        strategyDecorators.add(new ScopeDecorator(config.scopeBindings()));
        ProvisionStrategyDecorator compositeStrategyDecorator = new ProvisionStrategyDecorator() {
            @Override public <T> ProvisionStrategy<T> decorate(
                    ComponentMetadata<?> componentMetadata,
                    ProvisionStrategy<T> delegateStrategy,
                    Linkers linkers) {
                for (ProvisionStrategyDecorator decorator : strategyDecorators) {
                    delegateStrategy = decorator.decorate(componentMetadata, delegateStrategy, linkers);
                }
                return delegateStrategy;
            }
        };

        final ComponentAdapterFactory componentAdapterFactory =
                new ComponentAdapterFactoryImpl(injectorFactory, compositeStrategyDecorator);

        final HandlerFactory handlerFactory = new HandlerFactoryImpl(
                componentAdapterFactory,
                config.qualifierResolver(),
                config.componentMetadataResolver());

        final HandlerCache myCache = new HandlerCache();

        final GraphLinker graphLinker = new GraphLinker();

        for (Class<?> module : rootModule.modules()) {
            List<DependencyRequestHandler<?>> moduleRequestHandlers =
                    handlerFactory.createHandlersForModule(module, graphLinker);
            myCache.putAll(moduleRequestHandlers);
        }

        return new GraphImpl(
                config,
                graphLinker,
                injectorFactory,
                handlerFactory,
                myCache,
                parentCache);
    }

    private static <T> ProvisionStrategy<T> createProviderStrategy(final ProvisionStrategy<?> componentStrategy,
                                                            final Config config,
                                                            final InternalProvider internalProvider) {
        final Object provider = config.provider(new Provider<Object>() {

            final ThreadLocal<ResolutionContext> contextThreadLocal = new ThreadLocal<>();

            @Override public Object get() {

                ResolutionContext context = contextThreadLocal.get();

                if (context != null) {
                    return componentStrategy.get(
                            internalProvider, context);
                }

                try {
                    context = ResolutionContext.create();
                    contextThreadLocal.set(context);
                    return componentStrategy.get(
                            internalProvider, context);
                } finally {
                    contextThreadLocal.remove();
                }

            }

        });

        return (p, c) -> Generics.as(provider);
    }

    private static <T, C> ProvisionStrategy<T> createProviderStrategy(
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

    private static class HandlerCache {

        final Map<Dependency<?>, DependencyRequestHandler<?>> requestHandlers = new ConcurrentHashMap<>(64, .75f, 2);

        HandlerCache(HandlerCache ... caches) {
            for (HandlerCache cache : caches) {
                if (cache != null) {
                    requestHandlers.putAll(cache.requestHandlers);
                }
            }
        }

        void putAll(List<DependencyRequestHandler<?>> requestHandlers) {
            for (DependencyRequestHandler<?> requestHandler : requestHandlers) {
                putAll(requestHandler);
            }
        }

        <T> void putAll(DependencyRequestHandler<T> requestHandler) {
            for (Dependency<? super T> dependency : requestHandler.targets()) {
                put(dependency, requestHandler);
            }
        }

        <T> void put(final Dependency<? super T> dependency, DependencyRequestHandler<T> requestHandler) {
            ComponentMetadata<?> currentComponent = requestHandler.componentMetadata();
            if (currentComponent.isCollectionElement()) {
                putCollectionElement(dependency, requestHandler);
            } else {
                DependencyRequestHandler<?> previous = requestHandlers.get(dependency);
                if (previous != null) {
                    ComponentMetadata<?> previousComponent = previous.componentMetadata();
                    // TODO better messages.
                    // TODO This could randomly pass/fail in case of multiple non-override enabled
                    // TODO handlers with a single enabled handler.  Low priority.
                    if (previousComponent.isOverrideEnabled() && currentComponent.isOverrideEnabled()) {
                        throw new RuntimeException("more than one of type with override enabled");
                    } else if (!previousComponent.isOverrideEnabled() && !currentComponent.isOverrideEnabled()) {
                        throw new RuntimeException("more than one of type without override enabled");
                    } else if (currentComponent.isOverrideEnabled()) {
                        requestHandlers.put(dependency, requestHandler);
                    } // else keep previous
                } else {
                    requestHandlers.put(dependency, requestHandler);
                }
            }
        }

        <T> void putCollectionElement(final Dependency<T> dependency,
                                      DependencyRequestHandler<? extends T> requestHandler) {
            Dependency<List<T>> collectionDependency =
                    Dependency.from(dependency.qualifier(), new ParameterizedType() {
                        @Override public Type[] getActualTypeArguments() {
                            return new Type[] {dependency.typeKey().type()};
                        }

                        @Override public Type getRawType() {
                            return List.class;
                        }

                        @Override public Type getOwnerType() {
                            return null;
                        }

                        @Override public int hashCode() {
                            return Arrays.hashCode(getActualTypeArguments()) * 67 + getRawType().hashCode();
                        }

                        @Override public boolean equals(Object target) {
                            if (target == this) {
                                return true;
                            }
                            if (!(target instanceof ParameterizedType)) {
                                return false;
                            }
                            ParameterizedType parameterizedType = (ParameterizedType) target;
                            return parameterizedType.getRawType().equals(getRawType())
                                    && Arrays.equals(parameterizedType.getActualTypeArguments(), getActualTypeArguments());
                        }

                    });
            CollectionRequestHandler<T> collectionRequestHandler
                    = Generics.as(requestHandlers.get(collectionDependency));
            if (collectionRequestHandler == null) {
                collectionRequestHandler = new CollectionRequestHandler<>(collectionDependency, dependency);
                requestHandlers.put(collectionDependency, collectionRequestHandler);
            }
            collectionRequestHandler.requestHandlers.add(requestHandler);
        }

        <T> DependencyRequestHandler<? extends T> get(Dependency<T> dependency) {
            return Generics.as(requestHandlers.get(dependency));
        }

        static class CollectionRequestHandler<T> implements DependencyRequestHandler<List<T>> {

            private final List<DependencyRequestHandler<? extends T>> requestHandlers = new ArrayList<>();
            private final Dependency<List<T>> dependency;
            private final Dependency<T> subDependency;

            CollectionRequestHandler(Dependency<List<T>> dependency, Dependency<T> subDependency) {
                this.dependency = dependency;
                this.subDependency = subDependency;
            }

            @Override public List<Dependency<? super List<T>>> targets() {
                return Collections.<Dependency<? super List<T>>>singletonList(dependency);
            }

            @Override public List<Dependency<?>> dependencies() {
                List<Dependency<?>> dependencies = new LinkedList<>();
                for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
                    dependencies.addAll(requestHandler.dependencies());
                }
                return dependencies;
            }

            @Override public DependencyResponse<List<T>> handle(final DependencyRequest<? super List<T>> dependencyRequest) {
                return new DependencyResponse<List<T>>() {
                    @Override public ValidatedDependencyResponse<List<T>> validateResponse() {
                        DependencyRequest<T> subRequest =
                                DependencyRequest.create(dependencyRequest, subDependency);
                        for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
                            requestHandler.handle(subRequest).validateResponse();
                        }
                        return new ValidatedDependencyResponse<List<T>>() {
                            @Override public ProvisionStrategy<List<T>> getProvisionStrategy() {
                                return force();
                            }

                            @Override public ValidatedDependencyResponse<List<T>> validateResponse() {
                                return this;
                            }
                        };
                    }
                };
            }

            @Override public ProvisionStrategy<List<T>> force() {
                return (internalProvider, resolutionContext) -> {
                    List<T> list = new LinkedList<>();
                    for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
                        ProvisionStrategy<? extends T> provisionStrategy = requestHandler.force();
                        list.add(provisionStrategy.get(internalProvider, resolutionContext));
                    }
                    return list;
                };
            }

            @Override public ComponentMetadata<?> componentMetadata() {
                throw new UnsupportedOperationException(); // TODO exception message
            }

            @Override public DependencyRequestHandler<List<T>> newHandlerInstance(Linkers linkers) {
                CollectionRequestHandler<T> newHandler = new CollectionRequestHandler<>(dependency, subDependency);
                for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
                    newHandler.requestHandlers.add(requestHandler.newHandlerInstance(linkers));
                }
                return newHandler;
            }

        }

    }

    private static class GraphLinker implements Linkers, Linker {

        final Queue<WiringLinker> postWiringLinkers = new LinkedList<>();
        final Queue<EagerLinker> eagerLinkers = new LinkedList<>();

        @Override public void addWiringLinker(WiringLinker linker) {
            postWiringLinkers.add(linker);
        }

        @Override public void addEagerLinker(EagerLinker linker) {
            eagerLinkers.add(linker);
        }

        @Override public void link(InternalProvider internalProvider, ResolutionContext linkingContext) {
            while (!postWiringLinkers.isEmpty()) {
                postWiringLinkers.remove().link(internalProvider, linkingContext);
            }
            while (!eagerLinkers.isEmpty()) {
                eagerLinkers.remove().link(internalProvider, linkingContext);
            }
        }

    }

    private static class InternalProviderImpl implements InternalProvider {

        private final Config config;
        private final HandlerFactory handlerFactory;
        private final HandlerCache compositeCache;
        private final HandlerCache myCache;
        private final Linkers linkers;

        InternalProviderImpl(Config config,
                             HandlerFactory handlerFactory,
                             HandlerCache compositeCache,
                             HandlerCache myCache,
                             Linkers linkers) {
            this.config = config;
            this.handlerFactory = handlerFactory;
            this.compositeCache = compositeCache;
            this.myCache = myCache;
            this.linkers = linkers;
        }

        @Override public <T> ProvisionStrategy<? extends T> getProvisionStrategy(final DependencyRequest<T> dependencyRequest) {
            final Dependency<T> dependency = dependencyRequest.dependency();
            DependencyRequestHandler<? extends T> requestHandler = compositeCache.get(dependency);
            if (requestHandler != null) {
                return requestHandler
                        .handle(dependencyRequest)
                        .validateResponse()
                        .getProvisionStrategy();
            }
            if (config.isProvider(dependency)) {
                requestHandler = createProviderHandler(dependencyRequest, config, this, compositeCache);
                if (requestHandler != null) {
                    myCache.put(dependency, requestHandler);
                    compositeCache.put(dependency, requestHandler);
                    return requestHandler
                            .handle(dependencyRequest)
                            .validateResponse()
                            .getProvisionStrategy();
                }
            }
            requestHandler = handlerFactory.attemptToCreateHandlerFor(dependencyRequest, linkers);
            if (requestHandler != null) {
                // we do not cache handlers for specific requests - each request gets its own
                return requestHandler
                        .handle(dependencyRequest)
                        .validateResponse()
                        .getProvisionStrategy();
            }
            throw new DependencyException("missing dependency " + dependency.toString()); // TODO
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
                            componentHandler.handle(DependencyRequest.create(providerRequest, componentDependency));
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

                @Override public ComponentMetadata<?> componentMetadata() {
                    return componentHandler.componentMetadata();
                }

                @Override public DependencyRequestHandler<T> newHandlerInstance(Linkers linkers) {
                    // TODO important!!!!!!!!!!  implement ASAP
                    //TODO move internal provider into static class, make this method a part of of it?
                    throw new UnsupportedOperationException();
                }

            };

        }

    }

    private static class GraphImpl implements ObjectGraph {

        private final Config config;
        private final GraphLinker graphLinker;
        private final InternalProvider internalProvider;
        private final InjectorFactory injectorFactory;
        private final HandlerFactory handlerFactory;
        private final HandlerCache compositeCache;
        private final HandlerCache myCache;
        private final HandlerCache parentCache;
        private final Map<Class<?>, Injector<?>> injectors = new ConcurrentHashMap<>(16, .75f, 4);

        GraphImpl(Config config,
                  GraphLinker graphLinker,
                  InjectorFactory injectorFactory,
                  HandlerFactory handlerFactory,
                  HandlerCache myCache,
                  HandlerCache parentCache) {
            this.config = config;
            this.graphLinker = graphLinker;
            this.injectorFactory = injectorFactory;
            this.handlerFactory = handlerFactory;
            this.myCache = myCache;
            this.parentCache = parentCache;

            compositeCache = new HandlerCache(parentCache, myCache);

            internalProvider =
                    new InternalProviderImpl(config, handlerFactory, compositeCache, myCache, graphLinker);

            graphLinker.link(internalProvider, ResolutionContext.create());

        }

        @Override public <T> ObjectGraph inject(T injectionTarget) {

            final Class<T> targetClass = Generics.as(injectionTarget.getClass());

            Injector<T> injector = Generics.as(injectors.get(targetClass));

            if (injector == null) {

                final Qualifier qualifier = config.qualifierResolver().resolve(targetClass);

                injector = injectorFactory.compositeInjector(
                        config.componentMetadataResolver().resolveMetadata(
                                targetClass,
                                new ModuleMetadata(targetClass, qualifier, new Class<?>[0])),
                        graphLinker);

                graphLinker.link(internalProvider, ResolutionContext.create());

                injectors.put(targetClass, injector);
            }

            injector.inject(injectionTarget, internalProvider, ResolutionContext.create());

            return this;
        }

        @Override public <T, D extends io.gunmetal.Dependency<T>> T get(Class<D> dependencySpec) {
            Qualifier qualifier = config.qualifierResolver().resolve(dependencySpec);
            Type parameterizedDependencySpec = dependencySpec.getGenericInterfaces()[0];
            Type dependencyType = ((ParameterizedType) parameterizedDependencySpec).getActualTypeArguments()[0];
            Dependency<T> dependency = Dependency.from(
                    qualifier,
                    dependencyType);
            DependencyRequestHandler<? extends T> requestHandler = compositeCache.get(dependency);
            if (requestHandler != null) {
                return requestHandler.force().get(internalProvider, ResolutionContext.create());
            } else if (config.isProvider(dependency)) {
                ProvisionStrategy<T> providerStrategy =
                        createProviderStrategy(dependency, config, internalProvider, compositeCache);
                if (providerStrategy != null) {
                    return providerStrategy.get(internalProvider, ResolutionContext.create());
                }
            }
            return null;
        }

        @Override public ObjectGraph plus(Class<?> root) {
            return new GraphBuilderImpl().build(root, compositeCache);
        }

        @Override public ObjectGraph newInstance() {

            GraphLinker graphLinker = new GraphLinker();

            HandlerCache newMyCache = new HandlerCache();

            for (Map.Entry<Dependency<?>, DependencyRequestHandler<?>> entry : myCache.requestHandlers.entrySet()) {
                newMyCache.requestHandlers.put(
                        entry.getKey(),
                        entry.getValue().newHandlerInstance(graphLinker));
            }

            Map<Class<?>, Injector<?>> injectorHashMap = new HashMap<>();

            for (Map.Entry<Class<?>, Injector<?>> entry : injectors.entrySet()) {
                injectorHashMap.put(entry.getKey(), entry.getValue().newInjectorInstance(graphLinker));
            }

            GraphImpl copy = new GraphImpl(
                    config,
                    graphLinker,
                    injectorFactory,
                    handlerFactory,
                    newMyCache,
                    parentCache);

            copy.injectors.putAll(injectorHashMap);

            return copy;

        }

    }

}
