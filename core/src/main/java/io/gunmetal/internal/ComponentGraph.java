package io.gunmetal.internal;

import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResolutionContext;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * @author rees.byars
 */
final class ComponentGraph {

    private final ComponentLinker componentLinker;
    private final DependencySupplier dependencySupplier;
    private final ComponentContext componentContext;
    private final ComponentInjectors componentInjectors;
    private final Map<Method, ComponentMethodConfig> configMap;

    ComponentGraph(ComponentLinker componentLinker,
                   DependencySupplier dependencySupplier,
                   ComponentContext componentContext,
                   ComponentInjectors componentInjectors,
                   Map<Method, ComponentMethodConfig> configMap) {
        this.componentLinker = componentLinker;
        this.dependencySupplier = dependencySupplier;
        this.componentContext = componentContext;
        this.componentInjectors = componentInjectors;
        this.configMap = configMap;
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
                            componentInjectors
                                    .getInjector(arg, dependencySupplier, componentLinker, componentContext)
                                    .inject(arg, dependencySupplier, componentContext.newResolutionContext());
                        }
                        return null;
                    }
                    ComponentMethodConfig config = configMap.get(method);
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            resolutionContext.setParam(
                                    config.dependencies()[i],
                                    args[i]);
                        }
                    }
                    ProvisionStrategy strategy = dependencySupplier.supply(config.dependencyRequest());
                    if (strategy == null) {
                        // TODO no matching resource
                        throw new RuntimeException("not fucking here!");
                    }
                    return strategy.get(dependencySupplier, resolutionContext);
                }));
    }

}
