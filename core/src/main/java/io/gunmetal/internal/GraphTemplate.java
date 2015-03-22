package io.gunmetal.internal;

import io.gunmetal.ObjectGraph;
import io.gunmetal.TemplateGraph;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.ResolutionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rees.byars
 */
class GraphTemplate implements TemplateGraph {

    private final GraphConfig graphConfig;
    private final GraphInjectorProvider graphInjectorProvider;
    private final ProvisionStrategyDecorator strategyDecorator;
    private final DependencyServiceFactory dependencyServiceFactory;
    private final GraphCache graphCache;
    private final Set<Class<?>> loadedModules;

    private GraphTemplate(
            GraphConfig graphConfig,
            InjectorFactory injectorFactory,
            ProvisionStrategyDecorator strategyDecorator,
            DependencyServiceFactory dependencyServiceFactory,
            GraphCache graphCache,
            Set<Class<?>> loadedModules) {
        this.graphConfig = graphConfig;
        graphInjectorProvider = new GraphInjectorProvider(
                injectorFactory, graphConfig.getConfigurableMetadataResolver());
        this.strategyDecorator = strategyDecorator;
        this.dependencyServiceFactory = dependencyServiceFactory;
        this.graphCache = graphCache;
        this.loadedModules = loadedModules;
    }

    static TemplateGraph buildTemplate(Graph parentGraph, GraphConfig graphConfig, Class<?>... modules) {

        List<ProvisionStrategyDecorator> strategyDecorators = new ArrayList<>();
        if (parentGraph != null) {
            List<? extends ProvisionStrategyDecorator> parentDecorators =
                    parentGraph.get(ProvisionStrategyDecorator.DecoratorsDependency.class);
            if (parentDecorators != null) {
                strategyDecorators.addAll(parentDecorators);
            }
        }
        strategyDecorators.add(new ScopeDecorator(scope -> {
            ProvisionStrategyDecorator decorator = graphConfig.getScopeDecorators().get(scope);
            if (decorator != null) {
                return decorator;
            }
            throw new UnsupportedOperationException(); // TODO
        }));
        ProvisionStrategyDecorator strategyDecorator = (resourceMetadata, delegateStrategy, linkers) -> {
            for (ProvisionStrategyDecorator decorator : strategyDecorators) {
                delegateStrategy = decorator.decorate(resourceMetadata, delegateStrategy, linkers);
            }
            return delegateStrategy;
        };

        InjectorFactory injectorFactory = new InjectorFactoryImpl(
                graphConfig.getConfigurableMetadataResolver(),
                graphConfig.getConstructorResolver(),
                new ClassWalkerImpl(
                        graphConfig.getInjectionResolver(),
                        graphConfig.getGraphMetadata().isRestrictFieldInjection(),
                        graphConfig.getGraphMetadata().isRestrictSetterInjection()));

        ResourceFactory resourceFactory =
                new ResourceFactoryImpl(injectorFactory, graphConfig.getGraphMetadata().isRequireAcyclic());

        BindingFactory bindingFactory = new BindingFactoryImpl(
                resourceFactory,
                graphConfig.getConfigurableMetadataResolver(),
                graphConfig.getConfigurableMetadataResolver());

        RequestVisitorFactory requestVisitorFactory =
                new RequestVisitorFactoryImpl(
                        graphConfig.getConfigurableMetadataResolver(),
                        graphConfig.getGraphMetadata().isRequireExplicitModuleDependencies());

        DependencyServiceFactory dependencyServiceFactory =
                new DependencyServiceFactoryImpl(bindingFactory, requestVisitorFactory);

        GraphCache graphCache = new GraphCache(
                dependencyServiceFactory,
                parentGraph == null ? null : parentGraph.graphCache());

        GraphLinker graphLinker = new GraphLinker();
        GraphErrors errors = new GraphErrors();
        GraphContext graphContext = new GraphContext(
                ProvisionStrategyDecorator::none,
                graphLinker,
                errors,
                Collections.emptyMap()
        );
        Set<Class<?>> loadedModules = new HashSet<>();
        if (parentGraph != null) {
            loadedModules.addAll(parentGraph.loadedModules());
        }

        for (Class<?> module : modules) {
            List<DependencyService> moduleDependencyServices =
                    dependencyServiceFactory.createForModule(module, graphContext, loadedModules);
            graphCache.putAll(moduleDependencyServices, errors);
        }

        InternalProvider internalProvider =
                new GraphProvider(
                        graphConfig.getProviderAdapter(),
                        dependencyServiceFactory,
                        graphConfig.getConverterProvider(),
                        graphCache,
                        graphContext,
                        graphConfig.getGraphMetadata().isRequireInterfaces());

        graphLinker.linkGraph(internalProvider, ResolutionContext.create());
        errors.throwIfNotEmpty();

        return new GraphTemplate(
                graphConfig,
                injectorFactory,
                strategyDecorator,
                dependencyServiceFactory,
                graphCache,
                loadedModules);
    }

    @Override public ObjectGraph newInstance(Object... statefulModules) {

        Map<Class<?>, Object> statefulModulesMap = new HashMap<>();

        for (Object module : statefulModules) {
            statefulModulesMap.put(module.getClass(), module);
        }

        GraphLinker graphLinker = new GraphLinker();
        GraphErrors errors = new GraphErrors();
        GraphContext graphContext = new GraphContext(
                strategyDecorator,
                graphLinker,
                errors,
                statefulModulesMap
        );

        GraphCache newGraphCache = graphCache.replicateWith(graphContext);

        InternalProvider internalProvider =
                new GraphProvider(
                        graphConfig.getProviderAdapter(),
                        dependencyServiceFactory,
                        graphConfig.getConverterProvider(),
                        newGraphCache,
                        graphContext,
                        graphConfig.getGraphMetadata().isRequireInterfaces());

        GraphInjectorProvider injectorProvider = graphInjectorProvider.replicateWith(graphContext);

        graphLinker.linkAll(internalProvider, ResolutionContext.create());
        errors.throwIfNotEmpty();

        return new Graph(
                graphConfig,
                graphLinker,
                internalProvider,
                newGraphCache,
                graphContext,
                injectorProvider,
                loadedModules);

    }

}
