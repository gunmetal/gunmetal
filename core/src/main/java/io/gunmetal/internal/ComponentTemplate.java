package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Qualifier;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
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
    private final ResourceAccessorFactory resourceAccessorFactory;
    private final ComponentRepository componentRepository;
    private final Dependency[] providedDependencies;
    private final Map<Method, ComponentMethodConfig> componentMethodConfigs;

    private ComponentTemplate(
            Class<?> componentClass,
            ComponentConfig componentConfig,
            InjectorFactory injectorFactory,
            ProvisionStrategyDecorator strategyDecorator,
            ResourceAccessorFactory resourceAccessorFactory,
            ComponentRepository componentRepository,
            Dependency[] providedDependencies,
            Map<Method, ComponentMethodConfig> componentMethodConfigs) {
        this.componentClass = componentClass;
        this.componentConfig = componentConfig;
        componentInjectors = new ComponentInjectors(
                injectorFactory, componentConfig.getConfigurableMetadataResolver());
        this.strategyDecorator = strategyDecorator;
        this.resourceAccessorFactory = resourceAccessorFactory;
        this.componentRepository = componentRepository;
        this.providedDependencies = providedDependencies;
        this.componentMethodConfigs = componentMethodConfigs;
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

        ResourceAccessorFactory resourceAccessorFactory =
                new ResourceAccessorFactoryImpl(bindingFactory, requestVisitorFactory);

        ComponentRepository componentRepository = new ComponentRepository(
                resourceAccessorFactory,
                parentComponentGraph == null ? null : parentComponentGraph.repository());

        ComponentLinker componentLinker = new ComponentLinker();
        ComponentErrors errors = new ComponentErrors();
        ComponentContext componentContext = new ComponentContext(
                ProvisionStrategyDecorator::none,
                componentLinker,
                errors,
                Collections.emptyMap()
        );
        if (parentComponentGraph != null) {
            componentContext.loadedModules().addAll(
                    parentComponentGraph.context().loadedModules());
        }

        for (Class<?> module : modules) {
            List<ResourceAccessor> moduleResourceAccessors =
                    resourceAccessorFactory.createForModule(module, componentContext);
            componentRepository.putAll(moduleResourceAccessors, errors);
        }

        DependencySupplier dependencySupplier =
                new ComponentDependencySupplier(
                        componentConfig.getSupplierAdapter(),
                        resourceAccessorFactory,
                        componentConfig.getConverterSupplier(),
                        componentRepository,
                        componentContext,
                        componentConfig.getComponentSettings().isRequireInterfaces());

         // TODO move all this shit to a method or sumpin
        Map<Method, ComponentMethodConfig> componentMethodConfigs = new HashMap<>();
        Qualifier componentQualifier = componentConfig
                .getConfigurableMetadataResolver()
                .resolve(componentClass);
        for (Method method : componentClass.getDeclaredMethods()) {

            // TODO complete these checks
            if (method.getReturnType() == void.class ||
                    method.getName().equals("plus")) {
                continue;
            }

            Parameter[] parameters = method.getParameters();
            Dependency[] dependencies = new Dependency[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                ResourceAccessor resourceAccessor =
                        resourceAccessorFactory.createForParam(parameters[i], componentContext);
                if (!resourceAccessor.binding().resource().metadata().isParam()) {
                    throw new RuntimeException("ain't no @Param"); // TODO
                }
                dependencies[i] = resourceAccessor.binding().targets().get(0);
                componentRepository.putAll(
                        resourceAccessor,
                        errors);
            }

            Type type = method.getGenericReturnType();
            Dependency dependency = Dependency.from(
                    componentConfig.getConfigurableMetadataResolver()
                            .resolve(method)
                            .merge(componentQualifier),
                    type);
            ResourceAccessor resourceAccessor = componentRepository.get(dependency);
            if (resourceAccessor == null) {
                throw new RuntimeException("not fucking here!"); // TODO
            }
            componentMethodConfigs.put(method, new ComponentMethodConfig(resourceAccessor, dependencies));
        }

        componentLinker.linkGraph(dependencySupplier, componentContext.newResolutionContext());
        errors.throwIfNotEmpty();

        final ComponentTemplate template = new ComponentTemplate(
                componentClass,
                componentConfig,
                injectorFactory,
                strategyDecorator,
                resourceAccessorFactory,
                componentRepository,
                DependencyUtils.forParamTypes(
                        componentMethod,
                        componentConfig.getConfigurableMetadataResolver(),
                        Qualifier.NONE),
                componentMethodConfigs);

        return componentFactoryInterface.cast(Proxy.newProxyInstance(
                componentFactoryInterface.getClassLoader(),
                new Class<?>[]{componentFactoryInterface},
                (proxy, method, args) -> {
                    // TODO toString, hashCode etc
                    return template.newInstance(args == null ? new Object[]{} : args);
                }));
    }

    Object newInstance(Object... statefulModules) {

        Map<Dependency, Object> statefulModulesMap = new HashMap<>();

        for (int i = 0; i < statefulModules.length; i++) {
            statefulModulesMap.put(providedDependencies[i], statefulModules[i]);
        }

        ComponentLinker componentLinker = new ComponentLinker();
        ComponentErrors errors = new ComponentErrors();
        ComponentContext componentContext = new ComponentContext(
                strategyDecorator,
                componentLinker,
                errors,
                statefulModulesMap
        );

        ComponentRepository newComponentRepository =
                componentRepository.replicateWith(componentContext);

        DependencySupplier dependencySupplier =
                new ComponentDependencySupplier(
                        componentConfig.getSupplierAdapter(),
                        resourceAccessorFactory,
                        componentConfig.getConverterSupplier(),
                        newComponentRepository,
                        componentContext,
                        componentConfig.getComponentSettings().isRequireInterfaces());

        ComponentInjectors injectors = componentInjectors.replicateWith(componentContext);

        componentLinker.linkAll(dependencySupplier, componentContext.newResolutionContext());
        errors.throwIfNotEmpty();

        ComponentGraph componentGraph = new ComponentGraph(
                componentConfig,
                componentLinker,
                dependencySupplier,
                newComponentRepository,
                componentContext,
                injectors,
                componentMethodConfigs);

        return componentGraph.createProxy(componentClass);

    }

}
