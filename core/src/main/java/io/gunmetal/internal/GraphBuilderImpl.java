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
import io.gunmetal.TemplateGraph;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.ComponentMetadataResolver;
import io.gunmetal.spi.Config;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProviderAdapter;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.QualifierResolver;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.util.Generics;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rees.byars
 */
public class GraphBuilderImpl implements GraphBuilder {

    @Override public TemplateGraph build(Class<?> root) {
        return build(root, null);
    }

    private TemplateGraph build(Class<?> root, final HandlerCache parentCache) {

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

        return new Template(
                config.qualifierResolver(),
                config.componentMetadataResolver(),
                config.providerAdapter(),
                graphLinker,
                injectorFactory,
                handlerFactory,
                handlerCache);
    }

    private static class Template implements TemplateGraph {

        private final QualifierResolver qualifierResolver;
        private final ComponentMetadataResolver metadataResolver;
        private final ProviderAdapter providerAdapter;
        private final InternalProvider internalProvider;
        private final InjectorFactory injectorFactory;
        private final HandlerFactory handlerFactory;
        private final HandlerCache handlerCache;
        private final Map<Class<?>, Injector<?>> injectors = new ConcurrentHashMap<>(16, .75f, 4);

        Template(QualifierResolver qualifierResolver,
                 ComponentMetadataResolver metadataResolver,
                 ProviderAdapter providerAdapter,
                 GraphLinker graphLinker,
                 InjectorFactory injectorFactory,
                 HandlerFactory handlerFactory,
                 HandlerCache handlerCache) {
            this.qualifierResolver = qualifierResolver;
            this.metadataResolver = metadataResolver;
            this.providerAdapter = providerAdapter;
            this.injectorFactory = injectorFactory;
            this.handlerFactory = handlerFactory;
            this.handlerCache = handlerCache;

            internalProvider =
                    new InternalProviderImpl(providerAdapter, handlerFactory, handlerCache, graphLinker);

            graphLinker.linkGraph(internalProvider, ResolutionContext.create(Collections.emptyMap()));

        }

        @Override public ObjectGraph newGraph(Object... statefulModules) {

            GraphLinker graphLinker = new GraphLinker();

            HandlerCache newHandlerCache = handlerCache.replicate(graphLinker);

            Map<Class<?>, Injector<?>> injectorHashMap = new HashMap<>();

            for (Map.Entry<Class<?>, Injector<?>> entry : injectors.entrySet()) {
                injectorHashMap.put(entry.getKey(), entry.getValue().replicate(graphLinker));
            }

            Map<Class<?>, Object> statefulModulesMap = new HashMap<>();

            for (Object module : statefulModules) {
                statefulModulesMap.put(module.getClass(), module);
            }

            GraphImpl copy = new GraphImpl(
                    this,
                    graphLinker,
                    newHandlerCache,
                    statefulModulesMap);

            copy.injectors.putAll(injectorHashMap);

            return copy;

        }
    }

    private static class GraphImpl implements ObjectGraph {

        private final Template template;
        private final GraphLinker graphLinker;
        private final InternalProvider internalProvider;
        private final HandlerCache handlerCache;
        private final Map<Class<?>, Injector<?>> injectors = new ConcurrentHashMap<>(16, .75f, 4);
        private final Map<Class<?>, Object> statefulModules;

        GraphImpl(Template template,
                  GraphLinker graphLinker,
                  HandlerCache handlerCache,
                  Map<Class<?>, Object> statefulModules) {
            this.template = template;
            this.graphLinker = graphLinker;
            this.handlerCache = handlerCache;
            this.statefulModules = statefulModules;

            internalProvider =
                    new InternalProviderImpl(
                            template.providerAdapter,
                            template.handlerFactory,
                            handlerCache,
                            graphLinker);

            graphLinker.linkAll(internalProvider, ResolutionContext.create(statefulModules));

        }

        @Override public <T> ObjectGraph inject(T injectionTarget) {

            final Class<T> targetClass = Generics.as(injectionTarget.getClass());

            Injector<T> injector = Generics.as(injectors.get(targetClass));

            if (injector == null) {

                final Qualifier qualifier = template.qualifierResolver.resolve(targetClass);

                injector = template.injectorFactory.compositeInjector(
                        template.metadataResolver.resolveMetadata(
                                targetClass,
                                new ModuleMetadata(targetClass, qualifier, new Class<?>[0])),
                        graphLinker);

                graphLinker.linkAll(internalProvider, ResolutionContext.create(statefulModules));

                injectors.put(targetClass, injector);
                template.injectors.put(targetClass, injector);
            }

            injector.inject(injectionTarget, internalProvider, ResolutionContext.create());

            return this;
        }

        @Override public <T, D extends io.gunmetal.Dependency<T>> T get(Class<D> dependencySpec) {

            Qualifier qualifier = template.qualifierResolver.resolve(dependencySpec);
            Type parameterizedDependencySpec = dependencySpec.getGenericInterfaces()[0];
            Type dependencyType = ((ParameterizedType) parameterizedDependencySpec).getActualTypeArguments()[0];
            Dependency<T> dependency = Dependency.from(
                    qualifier,
                    dependencyType);

            DependencyRequestHandler<? extends T> requestHandler = handlerCache.get(dependency);

            if (requestHandler != null) {

                return requestHandler.force().get(internalProvider, ResolutionContext.create());

            } else if (template.providerAdapter.isProvider(dependency)) {

                Type providedType = ((ParameterizedType) dependency.typeKey().type()).getActualTypeArguments()[0];
                final Dependency<?> componentDependency = Dependency.from(dependency.qualifier(), providedType);
                final DependencyRequestHandler<?> componentHandler = handlerCache.get(componentDependency);
                if (componentHandler == null) {
                    return null;
                }
                ProvisionStrategy<?> componentStrategy = componentHandler.force();
                return new ProviderStrategyFactory(template.providerAdapter)
                        .<T>create(componentStrategy, internalProvider)
                        .get(internalProvider, ResolutionContext.create());

            }

            return null;
        }

        @Override public TemplateGraph plus(Class<?> root) {
            return new GraphBuilderImpl().build(root, handlerCache);
        }

        @Override public ObjectGraph newGraph(Object... statefulModules) {
            return template.newGraph(statefulModules);
        }

    }

}
