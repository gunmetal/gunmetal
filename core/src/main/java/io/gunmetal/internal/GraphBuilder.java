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
import io.gunmetal.TemplateGraph;
import io.gunmetal.spi.ClassWalker;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.ComponentMetadataResolver;
import io.gunmetal.spi.ConstructorResolver;
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
import io.gunmetal.spi.ScopeBindings;
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
public class GraphBuilder {
    
    private ClassWalker classWalker;
    private QualifierResolver qualifierResolver;
    private ComponentMetadataResolver componentMetadataResolver;
    private ConstructorResolver constructorResolver;
    private ScopeBindings scopeBindings;
    private ProviderAdapter providerAdapter;
    private HandlerCache parentCache;

    public GraphBuilder() {
        Defaults defaults = new Defaults();
        this.classWalker = defaults.classWalker();
        this.qualifierResolver = defaults.qualifierResolver();
        this.componentMetadataResolver = defaults.componentMetadataResolver();
        this.constructorResolver = defaults.constructorResolver();
        this.scopeBindings = defaults.scopeBindings();
        this.providerAdapter = defaults.providerAdapter();
    }

    GraphBuilder(HandlerCache parentCache) {
        this.parentCache = parentCache;
    }

    public GraphBuilder withClassWalker(ClassWalker classWalker) {
        this.classWalker = classWalker;
        return this;
    }

    public GraphBuilder withQualifierResolver(QualifierResolver qualifierResolver) {
        this.qualifierResolver = qualifierResolver;
        return this;
    }

    public GraphBuilder withComponentMetadataResolver(ComponentMetadataResolver componentMetadataResolver) {
        this.componentMetadataResolver = componentMetadataResolver;
        return this;
    }

    public GraphBuilder withConstructorResolver(ConstructorResolver constructorResolver) {
        this.constructorResolver = constructorResolver;
        return this;
    }

    public GraphBuilder withScopeBindings(ScopeBindings scopeBindings) {
        this.scopeBindings = scopeBindings;
        return this;
    }

    public GraphBuilder withProviderAdapter(ProviderAdapter providerAdapter) {
        this.providerAdapter = providerAdapter;
        return this;
    }

    public TemplateGraph build(Class<?> ... modules) {
        return build(modules, parentCache);
    }

    private TemplateGraph build(Class<?>[] modules, final HandlerCache parentCache) {

        InjectorFactory injectorFactory = new InjectorFactoryImpl(
                qualifierResolver,
                constructorResolver,
                classWalker);

        final List<ProvisionStrategyDecorator> strategyDecorators = new ArrayList<>();
        strategyDecorators.add(new ScopeDecorator(scopeBindings));
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
                qualifierResolver,
                componentMetadataResolver);

        final HandlerCache handlerCache = new HandlerCache(parentCache);

        final GraphLinker graphLinker = new GraphLinker();

        for (Class<?> module : modules) {
            List<DependencyRequestHandler<?>> moduleRequestHandlers =
                    handlerFactory.createHandlersForModule(module, graphLinker);
            handlerCache.putAll(moduleRequestHandlers);
        }

        return new Template(
                graphLinker,
                injectorFactory,
                handlerFactory,
                handlerCache);
    }

    private class Template implements TemplateGraph {

        private final InternalProvider internalProvider;
        private final InjectorFactory injectorFactory;
        private final HandlerFactory handlerFactory;
        private final HandlerCache handlerCache;
        private final Map<Class<?>, Injector<?>> injectors = new ConcurrentHashMap<>(16, .75f, 4);

        Template(GraphLinker graphLinker,
                 InjectorFactory injectorFactory,
                 HandlerFactory handlerFactory,
                 HandlerCache handlerCache) {
            this.injectorFactory = injectorFactory;
            this.handlerFactory = handlerFactory;
            this.handlerCache = handlerCache;

            internalProvider =
                    new InternalProviderImpl(providerAdapter, handlerFactory, handlerCache, graphLinker);

            graphLinker.linkGraph(internalProvider, ResolutionContext.create(Collections.emptyMap()));

        }

        @Override public ObjectGraph newInstance(Object... statefulModules) {

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

    private class GraphImpl implements ObjectGraph {

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
                            providerAdapter,
                            template.handlerFactory,
                            handlerCache,
                            graphLinker);

            graphLinker.linkAll(internalProvider, ResolutionContext.create(statefulModules));

        }

        @Override public <T> ObjectGraph inject(T injectionTarget) {

            final Class<T> targetClass = Generics.as(injectionTarget.getClass());

            Injector<T> injector = Generics.as(injectors.get(targetClass));

            if (injector == null) {

                final Qualifier qualifier = qualifierResolver.resolve(targetClass);

                injector = template.injectorFactory.compositeInjector(
                        componentMetadataResolver.resolveMetadata(
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

        @Override public <T> T inject(Provider<T> injectionTarget) {
            T t = injectionTarget.get();
            inject(t);
            return t;
        }

        @Override public <T> T inject(Class<T> injectionTarget) {
            Qualifier qualifier = qualifierResolver.resolve(injectionTarget);
            Instantiator<T> instantiator = template.injectorFactory.constructorInstantiator(
                    componentMetadataResolver.resolveMetadata(
                            injectionTarget,
                            new ModuleMetadata(injectionTarget, qualifier, new Class<?>[0])),
                    graphLinker);
            T t = instantiator.newInstance(internalProvider, ResolutionContext.create());
            inject(t);
            return t;
        }

        @Override public <T, D extends io.gunmetal.Dependency<T>> T get(Class<D> dependencySpec) {

            Qualifier qualifier = qualifierResolver.resolve(dependencySpec);
            Type parameterizedDependencySpec = dependencySpec.getGenericInterfaces()[0];
            Type dependencyType = ((ParameterizedType) parameterizedDependencySpec).getActualTypeArguments()[0];
            Dependency<T> dependency = Dependency.from(
                    qualifier,
                    dependencyType);

            DependencyRequestHandler<? extends T> requestHandler = handlerCache.get(dependency);

            if (requestHandler != null) {

                return requestHandler.force().get(internalProvider, ResolutionContext.create());

            } else if (providerAdapter.isProvider(dependency)) {

                Type providedType = ((ParameterizedType) dependency.typeKey().type()).getActualTypeArguments()[0];
                final Dependency<?> componentDependency = Dependency.from(dependency.qualifier(), providedType);
                final DependencyRequestHandler<?> componentHandler = handlerCache.get(componentDependency);
                if (componentHandler == null) {
                    return null;
                }
                ProvisionStrategy<?> componentStrategy = componentHandler.force();
                return new ProviderStrategyFactory(providerAdapter)
                        .<T>create(componentStrategy, internalProvider)
                        .get(internalProvider, ResolutionContext.create());

            }

            return null;
        }

        @Override public GraphBuilder plus() {
            return new GraphBuilder(handlerCache)
                    .withClassWalker(classWalker)
                    .withComponentMetadataResolver(componentMetadataResolver)
                    .withConstructorResolver(constructorResolver)
                    .withProviderAdapter(providerAdapter)
                    .withQualifierResolver(qualifierResolver)
                    .withScopeBindings(scopeBindings);
        }

        @Override public ObjectGraph newInstance(Object... statefulModules) {
            return template.newInstance(statefulModules);
        }

    }

}
