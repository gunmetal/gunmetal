package io.gunmetal.internal;

import io.gunmetal.Param;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResolutionContext;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
final class ComponentGraph {

    private final ComponentConfig componentConfig;
    private final ComponentLinker componentLinker;
    private final DependencySupplier dependencySupplier;
    private final ComponentRepository componentRepository;
    private final ComponentContext componentContext;
    private final ComponentInjectors componentInjectors;

    ComponentGraph(ComponentConfig componentConfig,
                   ComponentLinker componentLinker,
                   DependencySupplier dependencySupplier,
                   ComponentRepository componentRepository,
                   ComponentContext componentContext,
                   ComponentInjectors componentInjectors) {
        this.componentConfig = componentConfig;
        this.componentLinker = componentLinker;
        this.dependencySupplier = dependencySupplier;
        this.componentRepository = componentRepository;
        this.componentContext = componentContext;
        this.componentInjectors = componentInjectors;
    }

    ComponentContext context() {
        return componentContext;
    }

    ComponentRepository repository() {
        return componentRepository;
    }

    void inject(Object injectionTarget) {
        componentInjectors
                .getInjector(injectionTarget, dependencySupplier, componentLinker, componentContext)
                .inject(injectionTarget, dependencySupplier, ResolutionContext.create());
    }

    static class ComponentMethodConfig {
        private final ResourceAccessor resourceAccessor;
        private final Dependency[] dependencies;
        ComponentMethodConfig(ResourceAccessor resourceAccessor,
                              Dependency[] dependencies) {
            this.resourceAccessor = resourceAccessor;
            this.dependencies = dependencies;
        }
    }

    <T> T createProxy(Class<T> componentInterface) {

        Map<Method, ComponentMethodConfig> configMap = new HashMap<>();
        Qualifier componentQualifier = componentConfig
                .getConfigurableMetadataResolver()
                .resolve(componentInterface);
        for (Method method : componentInterface.getDeclaredMethods()) {
            // TODO complete these checks
            if (method.getReturnType() == void.class ||
                    method.getName().equals("plus")) {
                continue;
            }
            Dependency[] dependencies = DependencyUtils.forMethod(
                    method, componentConfig.getConfigurableMetadataResolver(), componentQualifier);

            // TODO should be rolled into qualifier wrapper class
            for (Dependency dependency : dependencies) {
                if (Arrays.stream(dependency.qualifier().qualifiers()).noneMatch(q -> q instanceof Param)) {
                    throw new RuntimeException("ain't no @Param"); // TODO
                }
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
            configMap.put(method, new ComponentMethodConfig(resourceAccessor, dependencies));
        }

        return componentInterface.cast(Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{componentInterface},
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
                    ResolutionContext resolutionContext = ResolutionContext.create();
                    if (method.getName().equals("inject")) {
                        // TODO validate etc, earlier
                        for (Object arg : args) {
                            inject(arg);
                        }
                        return null;
                    }
                    if (method.getReturnType() == ComponentBuilder.class &&
                            method.getName().equals("plus")) {
                        // TODO validate no params, do this earlier
                        return new ComponentBuilder(this, componentConfig);
                    }
                    ComponentMethodConfig config = configMap.get(method);
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            resolutionContext.setParam(
                                    config.dependencies[i],
                                    args[i]);
                        }
                    }
                    return config.resourceAccessor
                            .force()
                            .get(dependencySupplier, resolutionContext);
                }));
    }

}
