package io.gunmetal.internal;

import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.ResolutionContext;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
    private final Map<Method, ComponentMethodConfig> configMap;

    ComponentGraph(ComponentConfig componentConfig,
                   ComponentLinker componentLinker,
                   DependencySupplier dependencySupplier,
                   ComponentRepository componentRepository,
                   ComponentContext componentContext,
                   ComponentInjectors componentInjectors,
                   Map<Method, ComponentMethodConfig> configMap) {
        this.componentConfig = componentConfig;
        this.componentLinker = componentLinker;
        this.dependencySupplier = dependencySupplier;
        this.componentRepository = componentRepository;
        this.componentContext = componentContext;
        this.componentInjectors = componentInjectors;
        this.configMap = configMap;
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
                .inject(injectionTarget, dependencySupplier, componentContext.newResolutionContext());
    }

    <T> T createProxy(Class<T> componentInterface) {
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
                    ResolutionContext resolutionContext = componentContext.newResolutionContext();
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
                                    config.dependencies()[i],
                                    args[i]);
                        }
                    }
                    return config.resourceAccessor()
                            .force()
                            .get(dependencySupplier, resolutionContext);
                }));
    }

}
