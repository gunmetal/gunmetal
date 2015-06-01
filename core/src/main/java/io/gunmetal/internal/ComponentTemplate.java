package io.gunmetal.internal;

import io.gunmetal.Component;
import io.gunmetal.Module;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.GunmetalComponent;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.Option;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.spi.ResourceMetadata;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rees.byars
 */
public final class ComponentTemplate {

    private final Class<?> componentClass;
    private final GunmetalComponent gunmetalComponent;
    private final ComponentInjectors componentInjectors;
    private final ProvisionStrategyDecorator strategyDecorator;
    private final ResourceAccessorFactory resourceAccessorFactory;
    private final ComponentGraph componentGraph;
    private final Dependency[] providedDependencies;
    private final Map<Method, ComponentMethodConfig> componentMethodConfigs;
    private final ComponentContext templateContext;

    private ComponentTemplate(
            Class<?> componentClass,
            GunmetalComponent gunmetalComponent,
            ComponentInjectors componentInjectors,
            ProvisionStrategyDecorator strategyDecorator,
            ResourceAccessorFactory resourceAccessorFactory,
            ComponentGraph componentGraph,
            Dependency[] providedDependencies,
            Map<Method, ComponentMethodConfig> componentMethodConfigs,
            ComponentContext templateContext) {
        this.componentClass = componentClass;
        this.gunmetalComponent = gunmetalComponent;
        this.componentInjectors = componentInjectors;
        this.strategyDecorator = strategyDecorator;
        this.resourceAccessorFactory = resourceAccessorFactory;
        this.componentGraph = componentGraph;
        this.providedDependencies = providedDependencies;
        this.componentMethodConfigs = componentMethodConfigs;
        this.templateContext = templateContext;
    }

    public static <T> T build(Class<T> componentFactoryInterface) {
        return build(new GunmetalComponent.Default(), componentFactoryInterface);
    }

    public static <T> T build(GunmetalComponent gunmetalComponent, Class<T> componentFactoryInterface) {

        if (!componentFactoryInterface.isInterface()) {
            throw new IllegalArgumentException("no bueno"); // TODO message
        }

        Method[] factoryMethods = componentFactoryInterface.getDeclaredMethods();
        if (factoryMethods.length != 1 || factoryMethods[0].getReturnType() == void.class) {
            throw new IllegalArgumentException("too many methods"); // TODO message
        }
        Method componentMethod = factoryMethods[0];
        Class<?> componentClass = componentMethod.getReturnType();

        List<ProvisionStrategyDecorator> strategyDecorators = new ArrayList<>(gunmetalComponent.strategyDecorators());
        strategyDecorators.add(new ScopeDecorator(scope -> {
            ProvisionStrategyDecorator decorator = gunmetalComponent.scopeDecorators().get(scope);
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
                gunmetalComponent.qualifierResolver(),
                gunmetalComponent.constructorResolver(),
                new ClassWalkerImpl(
                        gunmetalComponent.injectionResolver(),
                        gunmetalComponent.options().contains(Option.RESTRICT_FIELD_INJECTION),
                        gunmetalComponent.options().contains(Option.RESTRICT_SETTER_INJECTION)));

        ResourceFactory resourceFactory =
                new ResourceFactoryImpl(
                        injectorFactory,
                        gunmetalComponent.options().contains(Option.REQUIRE_ACYCLIC));

        BindingFactory bindingFactory = new BindingFactoryImpl(
                resourceFactory,
                gunmetalComponent.qualifierResolver(),
                gunmetalComponent.resourceMetadataResolver());

        RequestVisitorFactory requestVisitorFactory =
                new RequestVisitorFactoryImpl(
                        gunmetalComponent.qualifierResolver(),
                        gunmetalComponent.requestVisitors(),
                        gunmetalComponent.options().contains(Option.REQUIRE_EXPLICIT_MODULE_DEPENDENCIES));

        ResourceAccessorFactory resourceAccessorFactory =
                new ResourceAccessorFactoryImpl(bindingFactory, requestVisitorFactory);

        ComponentGraph componentGraph =
                new ComponentGraph(resourceAccessorFactory);

        ComponentLinker componentLinker = new ComponentLinker();
        ComponentErrors errors = new ComponentErrors();
        ComponentContext componentContext = new ComponentContext(
                ProvisionStrategyDecorator::none,
                componentLinker,
                errors,
                Collections.emptyMap()
        );

        Set<Class<?>> modules = new HashSet<>();
        Module componentModuleAnnotation = componentClass.getAnnotation(Module.class);
        if (componentModuleAnnotation == null) {
            throw new RuntimeException("The component class [" + componentClass.getName()
                    + "] must be annotated with @Module");
        } else if (!componentModuleAnnotation.component()) {
            throw new RuntimeException("The component class [" + componentClass.getName()
                    + "] must have @Module(component=true)");
        }
        Component componentAnnotation = componentMethod.getAnnotation(Component.class);
        if (componentAnnotation != null) {
            Collections.addAll(modules, componentAnnotation.dependsOn());
        }
        Collections.addAll(modules, componentMethod.getParameterTypes());
        Collections.addAll(modules, componentModuleAnnotation.dependsOn());
        componentContext.loadedModules().addAll(modules);
        List<Class<?>> factoryParamTypes = Arrays.asList(componentMethod.getParameterTypes());
        for (Class<?> module : modules) {
            List<ResourceAccessor> moduleResourceAccessors =
                    resourceAccessorFactory.createForModule(
                            module,
                            factoryParamTypes.contains(module),
                            componentContext);
            componentGraph.putAll(moduleResourceAccessors, errors);
        }

        DependencySupplier dependencySupplier =
                new ComponentDependencySupplier(
                        gunmetalComponent.supplierAdapter(),
                        resourceAccessorFactory,
                        gunmetalComponent.converterSupplier(),
                        componentGraph,
                        componentContext,
                        gunmetalComponent.options().contains(Option.REQUIRE_INTERFACES));

         // TODO move all this shit to a method or sumpin
        Map<Method, ComponentMethodConfig> componentMethodConfigs = new HashMap<>();
        Qualifier componentQualifier = gunmetalComponent.qualifierResolver().resolve(componentClass);

        ModuleMetadata componentModuleMetadata =
                new ModuleMetadata(
                        componentClass,
                        componentQualifier,
                        componentModuleAnnotation);
        ResourceMetadata<Class<?>> componentMetadata =
                gunmetalComponent.resourceMetadataResolver().resolveMetadata(
                        componentClass,
                        componentModuleMetadata,
                        errors);

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
                componentGraph.putAll(
                        resourceAccessor,
                        errors);
            }

            Type type = method.getGenericReturnType();
            Dependency dependency = Dependency.from(
                    gunmetalComponent.qualifierResolver()
                            .resolve(method)
                            .merge(componentQualifier),
                    type);

            componentMethodConfigs.put(method, new ComponentMethodConfig(
                    DependencyRequest.create(componentMetadata, dependency), dependencies));
        }

        componentLinker.linkGraph(dependencySupplier, componentContext.newResolutionContext());
        errors.throwIfNotEmpty();

        Class<?>[] paramTypes = componentMethod.getParameterTypes();
        Dependency[] dependencies = new Dependency[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            Qualifier paramQualifier = gunmetalComponent.qualifierResolver()
                    .resolveDependencyQualifier(
                            paramType,
                            Qualifier.NONE);
            Dependency paramDependency =
                    Dependency.from(paramQualifier, paramType);
            dependencies[i] = paramDependency;
        }

        final ComponentTemplate template = new ComponentTemplate(
                componentClass,
                gunmetalComponent,
                new ComponentInjectors(
                        injectorFactory,
                        gunmetalComponent.qualifierResolver(),
                        gunmetalComponent.resourceMetadataResolver()),
                strategyDecorator,
                resourceAccessorFactory,
                componentGraph,
                dependencies,
                componentMethodConfigs,
                componentContext);

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
        componentContext.loadedModules().addAll(templateContext.loadedModules());

        ComponentGraph newComponentGraph =
                componentGraph.replicateWith(componentContext);

        DependencySupplier dependencySupplier =
                new ComponentDependencySupplier(
                        gunmetalComponent.supplierAdapter(),
                        resourceAccessorFactory,
                        gunmetalComponent.converterSupplier(),
                        newComponentGraph,
                        componentContext,
                        gunmetalComponent.options().contains(Option.REQUIRE_INTERFACES));

        ComponentInjectors injectors = componentInjectors.replicateWith(componentContext);

        componentLinker.linkAll(dependencySupplier, componentContext.newResolutionContext());
        errors.throwIfNotEmpty();

        return componentClass.cast(Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{componentClass},
                (proxy, method, args) -> {
                    if (Object.class == method.getDeclaringClass()) {
                        String name = method.getName();
                        if ("equals".equals(name)) {
                            return proxy == args[0];
                        } else if ("hashCode".equals(name)) {
                            return System.identityHashCode(proxy);
                        } else if ("toString".equals(name)) {
                            return proxy.getClass().getName() + "@" +
                                    Integer.toHexString(System.identityHashCode(proxy)) +
                                    "$GunmetalComponent";
                        } else {
                            throw new IllegalStateException(String.valueOf(method));
                        }
                    }
                    ResolutionContext resolutionContext = componentContext.newResolutionContext();
                    if (method.getName().equals("inject")) {
                        // TODO validate etc, earlier
                        for (Object arg : args) {
                            injectors
                                    .getInjector(arg, dependencySupplier, componentLinker, componentContext)
                                    .inject(arg, dependencySupplier, componentContext.newResolutionContext());
                        }
                        return null;
                    }
                    ComponentMethodConfig config = componentMethodConfigs.get(method);
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            resolutionContext.setParam(
                                    config.dependencies[i],
                                    args[i]);
                        }
                    }
                    ProvisionStrategy strategy = dependencySupplier.supply(config.dependencyRequest);
                    if (strategy == null) {
                        // TODO no matching resource
                        throw new RuntimeException("not fucking here!");
                    }
                    return strategy.get(dependencySupplier, resolutionContext);
                }));

    }

    private static class ComponentMethodConfig {

        final DependencyRequest dependencyRequest;
        final Dependency[] dependencies;

        ComponentMethodConfig(DependencyRequest dependencyRequest,
                              Dependency[] dependencies) {
            this.dependencyRequest = dependencyRequest;
            this.dependencies = dependencies;
        }

    }

    interface DefaultTemplate<T> {
        T create();
    }

}
