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
import io.gunmetal.RootModule;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.Config;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.InternalProvider;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        final HandlerCache handlerCache = new HandlerCache(parentCache);

        final GraphLinker graphLinker = new GraphLinker();

        for (Class<?> module : rootModule.modules()) {
            List<DependencyRequestHandler<?>> moduleRequestHandlers =
                    handlerFactory.createHandlersForModule(module, graphLinker);
            handlerCache.putAll(moduleRequestHandlers);
        }

        return new GraphImpl(
                config,
                graphLinker,
                injectorFactory,
                handlerFactory,
                handlerCache);
    }

    private static class GraphImpl implements ObjectGraph {

        private final Config config;
        private final GraphLinker graphLinker;
        private final InternalProvider internalProvider;
        private final InjectorFactory injectorFactory;
        private final HandlerFactory handlerFactory;
        private final HandlerCache handlerCache;
        private final Map<Class<?>, Injector<?>> injectors = new ConcurrentHashMap<>(16, .75f, 4);

        GraphImpl(Config config,
                  GraphLinker graphLinker,
                  InjectorFactory injectorFactory,
                  HandlerFactory handlerFactory,
                  HandlerCache handlerCache) {
            this.config = config;
            this.graphLinker = graphLinker;
            this.injectorFactory = injectorFactory;
            this.handlerFactory = handlerFactory;
            this.handlerCache = handlerCache;

            internalProvider =
                    new InternalProviderImpl(config, handlerFactory, handlerCache, graphLinker);

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
            DependencyRequestHandler<? extends T> requestHandler = handlerCache.get(dependency);
            if (requestHandler != null) {
                return requestHandler.force().get(internalProvider, ResolutionContext.create());
            } else if (config.isProvider(dependency)) {
                Type providedType = ((ParameterizedType) dependency.typeKey().type()).getActualTypeArguments()[0];
                final Dependency<?> componentDependency = Dependency.from(dependency.qualifier(), providedType);
                final DependencyRequestHandler<?> componentHandler = handlerCache.get(componentDependency);
                if (componentHandler == null) {
                    return null;
                }
                ProvisionStrategy<?> componentStrategy = componentHandler.force();
                return new ProviderStrategyFactory(config)
                        .<T>create(componentStrategy, internalProvider)
                        .get(internalProvider, ResolutionContext.create());
            }
            return null;
        }

        @Override public ObjectGraph plus(Class<?> root) {
            return new GraphBuilderImpl().build(root, handlerCache);
        }

        @Override public ObjectGraph newInstance() {

            GraphLinker graphLinker = new GraphLinker();

            HandlerCache newHandlerCache = handlerCache.newInstance(graphLinker);

            Map<Class<?>, Injector<?>> injectorHashMap = new HashMap<>();

            for (Map.Entry<Class<?>, Injector<?>> entry : injectors.entrySet()) {
                injectorHashMap.put(entry.getKey(), entry.getValue().newInjectorInstance(graphLinker));
            }

            GraphImpl copy = new GraphImpl(
                    config,
                    graphLinker,
                    injectorFactory,
                    handlerFactory,
                    newHandlerCache);

            copy.injectors.putAll(injectorHashMap);

            return copy;

        }

    }

}
