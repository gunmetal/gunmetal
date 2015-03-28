package io.gunmetal.internal;

import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.ResolutionContext;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
final class ComponentTemplate {

    private final Class<?> componentClass;
    private final ComponentConfig componentConfig;
    private final ComponentInjectors componentInjectors;
    private final ProvisionStrategyDecorator strategyDecorator;
    private final DependencyServiceFactory dependencyServiceFactory;
    private final ComponentRepository componentRepository;
    private final Set<Class<?>> loadedModules;

    private ComponentTemplate(
            Class<?> componentClass,
            ComponentConfig componentConfig,
            InjectorFactory injectorFactory,
            ProvisionStrategyDecorator strategyDecorator,
            DependencyServiceFactory dependencyServiceFactory,
            ComponentRepository componentRepository,
            Set<Class<?>> loadedModules) {
        this.componentClass = componentClass;
        this.componentConfig = componentConfig;
        componentInjectors = new ComponentInjectors(
                injectorFactory, componentConfig.getConfigurableMetadataResolver());
        this.strategyDecorator = strategyDecorator;
        this.dependencyServiceFactory = dependencyServiceFactory;
        this.componentRepository = componentRepository;
        this.loadedModules = loadedModules;
    }

    static <T> T buildTemplate(ComponentGraph parentComponentGraph,
                               ComponentConfig componentConfig,
                               Class<T> componentFactoryInterface) {

        if (!componentFactoryInterface.isInterface()) {
            throw new IllegalArgumentException("no bueno"); // TODO message
        }

        Method[] factoryMethods = componentFactoryInterface.getDeclaredMethods();
        if (factoryMethods.length != 1 || factoryMethods[0].getReturnType() == void.class) {
            throw new IllegalArgumentException("too many methods"); // TODO message
        }
        Method componentMethod = factoryMethods[0];
        Class<?> componentClass = componentMethod.getReturnType();

        Set<Class<?>> modules = new HashSet<>();
        modules.add(componentClass);
        Collections.addAll(modules, componentMethod.getParameterTypes());

        ComponentExtensions componentExtensions = new ComponentExtensions();
        if (parentComponentGraph != null) {
             parentComponentGraph.inject(componentExtensions);
        }

        List<ProvisionStrategyDecorator> strategyDecorators = new ArrayList<>(componentExtensions.strategyDecorators());
        strategyDecorators.add(new ScopeDecorator(scope -> {
            ProvisionStrategyDecorator decorator = componentConfig.getScopeDecorators().get(scope);
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
                componentConfig.getConfigurableMetadataResolver(),
                componentConfig.getConstructorResolver(),
                new ClassWalkerImpl(
                        componentConfig.getInjectionResolver(),
                        componentConfig.getComponentSettings().isRestrictFieldInjection(),
                        componentConfig.getComponentSettings().isRestrictSetterInjection()));

        ResourceFactory resourceFactory =
                new ResourceFactoryImpl(injectorFactory, componentConfig.getComponentSettings().isRequireAcyclic());

        BindingFactory bindingFactory = new BindingFactoryImpl(
                resourceFactory,
                componentConfig.getConfigurableMetadataResolver(),
                componentConfig.getConfigurableMetadataResolver());

        RequestVisitorFactory requestVisitorFactory =
                new RequestVisitorFactoryImpl(
                        componentConfig.getConfigurableMetadataResolver(),
                        componentConfig.getComponentSettings().isRequireExplicitModuleDependencies());

        DependencyServiceFactory dependencyServiceFactory =
                new DependencyServiceFactoryImpl(bindingFactory, requestVisitorFactory);

        ComponentRepository componentRepository = new ComponentRepository(
                dependencyServiceFactory,
                parentComponentGraph == null ? null : parentComponentGraph.graphCache());

        ComponentLinker componentLinker = new ComponentLinker();
        ComponentErrors errors = new ComponentErrors();
        ComponentContext componentContext = new ComponentContext(
                ProvisionStrategyDecorator::none,
                componentLinker,
                errors,
                Collections.emptyMap()
        );
        Set<Class<?>> loadedModules = new HashSet<>();
        if (parentComponentGraph != null) {
            loadedModules.addAll(parentComponentGraph.loadedModules());
        }

        for (Class<?> module : modules) {
            List<DependencyService> moduleDependencyServices =
                    dependencyServiceFactory.createForModule(module, componentContext, loadedModules);
            componentRepository.putAll(moduleDependencyServices, errors);
        }

        DependencySupplier dependencySupplier =
                new ComponentDependencySupplier(
                        componentConfig.getSupplierAdapter(),
                        dependencyServiceFactory,
                        componentConfig.getConverterSupplier(),
                        componentRepository,
                        componentContext,
                        componentConfig.getComponentSettings().isRequireInterfaces());

        componentLinker.linkGraph(dependencySupplier, ResolutionContext.create());
        errors.throwIfNotEmpty();

        final ComponentTemplate template = new ComponentTemplate(
                componentClass,
                componentConfig,
                injectorFactory,
                strategyDecorator,
                dependencyServiceFactory,
                componentRepository,
                loadedModules);

        return componentFactoryInterface.cast(Proxy.newProxyInstance(
                componentFactoryInterface.getClassLoader(),
                new Class<?>[]{componentFactoryInterface},
                (proxy, method, args) -> {
                    // TODO toString, hashCode etc
                    return template.newInstance(args == null ? new Object[]{} : args);
                }));
    }

    Object newInstance(Object... statefulModules) {

        Map<Class<?>, Object> statefulModulesMap = new HashMap<>();

        for (Object module : statefulModules) {
            statefulModulesMap.put(module.getClass(), module);
        }

        ComponentLinker componentLinker = new ComponentLinker();
        ComponentErrors errors = new ComponentErrors();
        ComponentContext componentContext = new ComponentContext(
                strategyDecorator,
                componentLinker,
                errors,
                statefulModulesMap
        );

        ComponentRepository newComponentRepository = componentRepository.replicateWith(componentContext);

        DependencySupplier dependencySupplier =
                new ComponentDependencySupplier(
                        componentConfig.getSupplierAdapter(),
                        dependencyServiceFactory,
                        componentConfig.getConverterSupplier(),
                        newComponentRepository,
                        componentContext,
                        componentConfig.getComponentSettings().isRequireInterfaces());

        ComponentInjectors injectors = componentInjectors.replicateWith(componentContext);

        componentLinker.linkAll(dependencySupplier, ResolutionContext.create());
        errors.throwIfNotEmpty();

        ComponentGraph componentGraph = new ComponentGraph(
                componentConfig,
                componentLinker,
                dependencySupplier,
                newComponentRepository,
                componentContext,
                injectors,
                loadedModules);

        return componentGraph.createProxy(componentClass);

    }

}
