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

import io.gunmetal.Inject;
import io.gunmetal.ObjectGraph;
import io.gunmetal.Provider;
import io.gunmetal.TemplateGraph;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.InjectionResolver;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProviderAdapter;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.spi.Scope;
import io.gunmetal.spi.Scopes;
import io.gunmetal.spi.impl.AnnotationInjectionResolver;
import io.gunmetal.spi.impl.ExactlyOneConstructorResolver;
import io.gunmetal.spi.impl.GunmetalProviderAdapter;
import io.gunmetal.spi.impl.Jsr330ProviderAdapter;
import io.gunmetal.spi.impl.LeastGreedyConstructorResolver;
import io.gunmetal.util.Generics;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rees.byars
 */
public final class GraphBuilder {

    private MutableGraphMetadata graphMetadata;
    private InjectionResolver injectionResolver;
    private ConfigurableMetadataResolver configurableMetadataResolver;
    private ConstructorResolver constructorResolver;
    private ProviderAdapter providerAdapter;
    private Map<Scope, ProvisionStrategyDecorator> scopeDecorators;
    private Graph parentGraph;

    public GraphBuilder() {
        graphMetadata = new MutableGraphMetadata();
        injectionResolver = new AnnotationInjectionResolver(Inject.class);
        configurableMetadataResolver = new ConfigurableMetadataResolver();
        constructorResolver = new LeastGreedyConstructorResolver();
        providerAdapter = new GunmetalProviderAdapter();
        scopeDecorators = new HashMap<>();
        scopeDecorators.put(Scopes.UNDEFINED, ProvisionStrategyDecorator::none);
    }

    private GraphBuilder(Graph parentGraph,
                         MutableGraphMetadata graphMetadata,
                         InjectionResolver injectionResolver,
                         ConfigurableMetadataResolver configurableMetadataResolver,
                         ConstructorResolver constructorResolver,
                         ProviderAdapter providerAdapter,
                         Map<Scope, ProvisionStrategyDecorator> scopeDecorators) {
        this.parentGraph = parentGraph;
        this.graphMetadata = graphMetadata;
        this.injectionResolver = injectionResolver;
        this.configurableMetadataResolver = configurableMetadataResolver;
        this.constructorResolver = constructorResolver;
        this.providerAdapter = providerAdapter;
        this.scopeDecorators = scopeDecorators;
    }

    private GraphBuilder plus(Graph parentGraph) {
        return new GraphBuilder(
                parentGraph,
                graphMetadata.replicate(),
                injectionResolver,
                configurableMetadataResolver.replicate(),
                constructorResolver,
                providerAdapter,
                new HashMap<>(scopeDecorators));
    }

    public GraphBuilder requireQualifiers() {
        configurableMetadataResolver.requireQualifiers(true);
        return this;
    }

    public GraphBuilder restrictPluralQualifiers() {
        configurableMetadataResolver.restrictPluralQualifiers(true);
        return this;
    }

    public GraphBuilder requireInterfaces() {
        graphMetadata.setRequireInterfaces(true);
        return this;
    }

    public GraphBuilder requireAcyclic() {
        graphMetadata.setRequireAcyclic(true);
        return this;
    }

    public GraphBuilder requireExplicitModuleDependencies() {
        graphMetadata.setRequireExplicitModuleDependencies(true);
        return this;
    }

    public GraphBuilder restrictFieldInjection() {
        graphMetadata.setRestrictFieldInjection(true);
        return this;
    }

    public GraphBuilder restrictSetterInjection() {
        graphMetadata.setRestrictSetterInjection(true);
        return this;
    }

    public GraphBuilder withQualifierType(Class<? extends Annotation> qualifierType) {
        configurableMetadataResolver.qualifierType(qualifierType);
        return this;
    }

    public GraphBuilder withEagerType(Class<? extends Annotation> eagerType, boolean indicatesEager) {
        configurableMetadataResolver.eagerType(eagerType, indicatesEager);
        return this;
    }

    public GraphBuilder addScope(Class<? extends Annotation> scopeType,
                                 Scope scope,
                                 ProvisionStrategyDecorator scopeDecorator) {
        configurableMetadataResolver.addScope(scopeType, scope);
        scopeDecorators.put(scope, scopeDecorator);
        return this;
    }

    public GraphBuilder withJsr330Metadata() {
        configurableMetadataResolver
                .scopeType(javax.inject.Scope.class)
                .addScope(javax.inject.Singleton.class, Scopes.SINGLETON)
                .addScope(null, Scopes.PROTOTYPE)
                .qualifierType(javax.inject.Qualifier.class)
                .restrictPluralQualifiers(true);
        return withInjectionResolver(new AnnotationInjectionResolver(javax.inject.Inject.class))
                .withConstructorResolver(new ExactlyOneConstructorResolver(injectionResolver))
                .withProviderAdapter(new Jsr330ProviderAdapter());
    }

    public GraphBuilder withInjectionResolver(InjectionResolver injectionResolver) {
        this.injectionResolver = injectionResolver;
        return this;
    }

    public GraphBuilder withConstructorResolver(ConstructorResolver constructorResolver) {
        this.constructorResolver = constructorResolver;
        return this;
    }

    public GraphBuilder withProviderAdapter(ProviderAdapter providerAdapter) {
        this.providerAdapter = providerAdapter;
        return this;
    }

    public TemplateGraph buildTemplate(Class<?>... modules) {

        List<ProvisionStrategyDecorator> strategyDecorators = new ArrayList<>();
        if (parentGraph != null) {
            List<? extends ProvisionStrategyDecorator> parentDecorators =
                    parentGraph.get(ProvisionStrategyDecorator.DecoratorsDependency.class);
            if (parentDecorators != null) {
                strategyDecorators.addAll(parentDecorators);
            }
        }
        strategyDecorators.add(new ScopeDecorator(scope -> {
            ProvisionStrategyDecorator decorator = scopeDecorators.get(scope);
            if (decorator != null) {
                return decorator;
            }
            throw new UnsupportedOperationException();
        }));
        ProvisionStrategyDecorator strategyDecorator = new ProvisionStrategyDecorator() {
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

        InjectorFactory injectorFactory = new InjectorFactoryImpl(
                configurableMetadataResolver,
                constructorResolver,
                new ClassWalkerImpl(
                        injectionResolver,
                        graphMetadata.isRestrictFieldInjection(),
                        graphMetadata.isRestrictSetterInjection()));

        ComponentAdapterFactory componentAdapterFactory =
                new ComponentAdapterFactoryImpl(injectorFactory, graphMetadata.isRequireAcyclic());

        HandlerFactory handlerFactory = new HandlerFactoryImpl(
                componentAdapterFactory,
                configurableMetadataResolver,
                configurableMetadataResolver,
                graphMetadata.isRequireExplicitModuleDependencies());

        HandlerCache handlerCache = new HandlerCache(parentGraph == null ? null : parentGraph.handlerCache);

        GraphLinker graphLinker = new GraphLinker();
        GraphContext graphContext = GraphContext.create(
                ProvisionStrategyDecorator::none,
                graphLinker,
                Collections.emptyMap()
        );
        Set<Class<?>> loadedModules = new HashSet<>();
        if (parentGraph != null) {
            loadedModules.addAll(parentGraph.template.loadedModules);
        }

        for (Class<?> module : modules) {
            List<DependencyRequestHandler<?>> moduleRequestHandlers =
                    handlerFactory.createHandlersForModule(module, graphContext, loadedModules);
            handlerCache.putAll(moduleRequestHandlers);
        }

        InternalProvider internalProvider =
                new InternalProviderImpl(
                        providerAdapter,
                        handlerFactory,
                        handlerCache,
                        graphContext,
                        graphMetadata.isRequireInterfaces());

        graphLinker.linkGraph(internalProvider, ResolutionContext.create());

        return new Template(
                injectorFactory,
                strategyDecorator,
                handlerFactory,
                handlerCache,
                loadedModules);
    }

    private class Template implements TemplateGraph {

        private final InjectorFactory injectorFactory;
        private final ProvisionStrategyDecorator strategyDecorator;
        private final HandlerFactory handlerFactory;
        private final HandlerCache handlerCache;
        private final Set<Class<?>> loadedModules;
        private final Map<Class<?>, Injector<?>> injectors = new ConcurrentHashMap<>(16, .75f, 4);
        private final Map<Class<?>, Instantiator<?>> instantiators = new ConcurrentHashMap<>(16, .75f, 4);

        Template(InjectorFactory injectorFactory,
                 ProvisionStrategyDecorator strategyDecorator,
                 HandlerFactory handlerFactory,
                 HandlerCache handlerCache,
                 Set<Class<?>> loadedModules) {
            this.injectorFactory = injectorFactory;
            this.strategyDecorator = strategyDecorator;
            this.handlerFactory = handlerFactory;
            this.handlerCache = handlerCache;
            this.loadedModules = loadedModules;
        }

        @Override public ObjectGraph newInstance(Object... statefulModules) {

            GraphLinker graphLinker = new GraphLinker();

            Map<Class<?>, Object> statefulModulesMap = new HashMap<>();

            for (Object module : statefulModules) {
                statefulModulesMap.put(module.getClass(), module);
            }

            GraphContext graphContext = GraphContext.create(
                    strategyDecorator,
                    graphLinker,
                    statefulModulesMap
            );

            HandlerCache newHandlerCache = handlerCache.replicateWith(graphContext);

            Map<Class<?>, Injector<?>> injectorHashMap = new HashMap<>();
            for (Map.Entry<Class<?>, Injector<?>> entry : injectors.entrySet()) {
                injectorHashMap.put(entry.getKey(), entry.getValue().replicateWith(graphContext));
            }

            Map<Class<?>, Instantiator<?>> instantiatorHashMap = new HashMap<>();
            for (Map.Entry<Class<?>, Instantiator<?>> entry : instantiators.entrySet()) {
                instantiatorHashMap.put(entry.getKey(), entry.getValue().replicateWith(graphContext));
            }

            InternalProvider internalProvider =
                    new InternalProviderImpl(
                            providerAdapter,
                            handlerFactory,
                            newHandlerCache,
                            graphContext,
                            graphMetadata.isRequireInterfaces());

            graphLinker.linkAll(internalProvider, ResolutionContext.create());

            Graph copy = new Graph(
                    this,
                    graphLinker,
                    internalProvider,
                    newHandlerCache,
                    graphContext);

            copy.injectors.putAll(injectorHashMap);
            copy.instantiators.putAll(instantiatorHashMap);

            return copy;

        }
    }

    private class Graph implements ObjectGraph {

        private final Template template;
        private final GraphLinker graphLinker;
        private final InternalProvider internalProvider;
        private final HandlerCache handlerCache;
        private final Map<Class<?>, Injector<?>> injectors = new ConcurrentHashMap<>(16, .75f, 4);
        private final Map<Class<?>, Instantiator<?>> instantiators = new ConcurrentHashMap<>(16, .75f, 4);
        private final GraphContext graphContext;

        Graph(Template template,
              GraphLinker graphLinker,
              InternalProvider internalProvider,
              HandlerCache handlerCache,
              GraphContext graphContext) {
            this.template = template;
            this.graphLinker = graphLinker;
            this.internalProvider = internalProvider;
            this.handlerCache = handlerCache;
            this.graphContext = graphContext;
        }

        @Override public <T> ObjectGraph inject(T injectionTarget) {

            final Class<T> targetClass = Generics.as(injectionTarget.getClass());

            Injector<T> injector = Generics.as(injectors.get(targetClass));

            if (injector == null) {

                final Qualifier qualifier = configurableMetadataResolver.resolve(targetClass);

                injector = template.injectorFactory.compositeInjector(
                        configurableMetadataResolver.resolveMetadata(
                                targetClass,
                                new ModuleMetadata(targetClass, qualifier, new Class<?>[0])),
                        graphContext);

                graphLinker.linkAll(internalProvider, ResolutionContext.create());

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

            Instantiator<T> instantiator = Generics.as(instantiators.get(injectionTarget));

            if (instantiator == null) {

                final Qualifier qualifier = configurableMetadataResolver.resolve(injectionTarget);

                instantiator = template.injectorFactory.constructorInstantiator(
                        configurableMetadataResolver.resolveMetadata(
                                injectionTarget,
                                new ModuleMetadata(injectionTarget, qualifier, new Class<?>[0])),
                        graphContext);

                graphLinker.linkAll(internalProvider, ResolutionContext.create());

                instantiators.put(injectionTarget, instantiator);
                template.instantiators.put(injectionTarget, instantiator);
            }
            T t = instantiator.newInstance(internalProvider, ResolutionContext.create());
            inject(t);
            return t;
        }

        @Override public <T, D extends io.gunmetal.Dependency<T>> T get(Class<D> dependencySpec) {

            Qualifier qualifier = configurableMetadataResolver.resolve(dependencySpec);
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
            return GraphBuilder.this.plus(this);
        }

        @Override public ObjectGraph newInstance(Object... statefulModules) {
            return template.newInstance(statefulModules);
        }

    }

}
