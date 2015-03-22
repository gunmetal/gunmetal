package io.gunmetal.internal;

import io.gunmetal.ObjectGraph;
import io.gunmetal.Provider;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.util.Generics;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author rees.byars
 */
class Graph implements ObjectGraph {

    private final GraphConfig graphConfig;
    private final GraphLinker graphLinker;
    private final InternalProvider internalProvider;
    private final GraphCache graphCache;
    private final GraphContext graphContext;
    private final GraphInjectorProvider graphInjectorProvider;
    private final Set<Class<?>> loadedModules;

    Graph(GraphConfig graphConfig,
          GraphLinker graphLinker,
          InternalProvider internalProvider,
          GraphCache graphCache,
          GraphContext graphContext,
          GraphInjectorProvider graphInjectorProvider,
          Set<Class<?>> loadedModules) {
        this.graphConfig = graphConfig;
        this.graphLinker = graphLinker;
        this.internalProvider = internalProvider;
        this.graphCache = graphCache;
        this.graphContext = graphContext;
        this.graphInjectorProvider = graphInjectorProvider;
        this.loadedModules = loadedModules;
    }

    @Override public <T> ObjectGraph inject(T injectionTarget) {

        graphInjectorProvider
                .getInjector(injectionTarget, internalProvider, graphLinker, graphContext)
                .inject(injectionTarget, internalProvider, ResolutionContext.create());

        return this;
    }

    @Override public <T> T inject(Provider<T> injectionTarget) {
        T t = injectionTarget.get();
        inject(t);
        return t;
    }

    @Override public <T> T inject(Class<T> injectionTarget) {

        Object t = graphInjectorProvider
                .getInstantiator(injectionTarget, internalProvider, graphLinker, graphContext)
                .newInstance(internalProvider, ResolutionContext.create());

        inject(t);

        return Generics.as(t);

    }

    @Override public <T, D extends io.gunmetal.Dependency<T>> T get(Class<D> dependencySpec) {

        Qualifier qualifier = graphConfig.getConfigurableMetadataResolver()
                .resolve(dependencySpec, graphContext.errors());
        Type parameterizedDependencySpec = dependencySpec.getGenericInterfaces()[0];
        Type dependencyType = ((ParameterizedType) parameterizedDependencySpec).getActualTypeArguments()[0];
        Dependency dependency = Dependency.from(
                qualifier,
                dependencyType);

        DependencyService dependencyService = graphCache.get(dependency);

        if (dependencyService != null) {

            return Generics.as(dependencyService.force().get(internalProvider, ResolutionContext.create()));

        } else if (graphConfig.getProviderAdapter().isProvider(dependency)) {

            Type providedType = ((ParameterizedType) dependency.typeKey().type()).getActualTypeArguments()[0];
            final Dependency provisionDependency = Dependency.from(dependency.qualifier(), providedType);
            final DependencyService provisionDependencyService = graphCache.get(provisionDependency);
            if (provisionDependencyService == null) {
                return null;
            }
            return Generics.as(new ProviderStrategyFactory(graphConfig.getProviderAdapter())
                    .create(provisionDependencyService.force(), internalProvider)
                    .get(internalProvider, ResolutionContext.create()));

        }

        return null;
    }

    @Override public <T> T create(Class<T> componentInterface) {

        Map<Method, DependencyService> dependencyServiceMap = new HashMap<>();
        for (Method method : componentInterface.getDeclaredMethods()) {
            if (method.getParameterCount() > 0) {
                throw new RuntimeException("ain't no params allowed"); // TODO
            }
            Type type = method.getGenericReturnType();
            Dependency dependency = Dependency.from(
                    graphConfig.getConfigurableMetadataResolver()
                            .resolve(method, graphContext.errors()),
                    type);
            DependencyService dependencyService = graphCache.get(dependency);
            if (dependencyService == null) {
                throw new RuntimeException("not fucking here!"); // TODO
            }
            dependencyServiceMap.put(method, dependencyService);
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
                    return dependencyServiceMap
                            .get(method)
                            .force()
                            .get(internalProvider, ResolutionContext.create());
                }));
    }

    @Override public GraphBuilder plus() {
        return new GraphBuilder(this, graphConfig);
    }

    GraphCache graphCache() {
        return graphCache;
    }

    Set<Class<?>> loadedModules() {
        return loadedModules;
    }

}
