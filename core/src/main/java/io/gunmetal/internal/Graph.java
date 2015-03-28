package io.gunmetal.internal;

import io.gunmetal.Module;
import io.gunmetal.ObjectGraph;
import io.gunmetal.Param;
import io.gunmetal.Provider;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.spi.ResourceMetadata;
import io.gunmetal.util.Generics;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
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

    static class ComponentMethodConfig {
        private final DependencyService dependencyService;
        private final Dependency[] dependencies;
        ComponentMethodConfig(DependencyService dependencyService,
                              Dependency[] dependencies) {
            this.dependencyService = dependencyService;
            this.dependencies = dependencies;
        }
    }

    @Override public <T> T create(Class<T> componentInterface) {

        Map<Method, ComponentMethodConfig> configMap = new HashMap<>();
        Qualifier componentQualifier = graphConfig
                .getConfigurableMetadataResolver()
                .resolve(componentInterface, graphContext.errors());
        for (Method method : componentInterface.getDeclaredMethods()) {
            Type[] paramTypes = method.getGenericParameterTypes();
            final Annotation[][] methodParameterAnnotations
                    = method.getParameterAnnotations();
            Dependency[] dependencies = new Dependency[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                Type paramType = paramTypes[i];
                Annotation[] annotations = methodParameterAnnotations[i];
                AnnotatedElement annotatedElement =
                        new AnnotatedElement() {
                            @Override public <TT extends Annotation> TT getAnnotation(Class<TT> annotationClass) {
                                for (Annotation annotation : annotations) {
                                    if (annotationClass.isInstance(annotation)) {
                                        return annotationClass.cast(annotation);
                                    }
                                }
                                return null;
                            }
                            @Override public Annotation[] getAnnotations() {
                                return annotations;
                            }

                            @Override public Annotation[] getDeclaredAnnotations() {
                                return annotations;
                            }
                        };
                if (!annotatedElement.isAnnotationPresent(Param.class)) {
                    throw new RuntimeException("ain't no @Param"); // TODO
                }
                Qualifier paramQualifier = graphConfig
                        .getConfigurableMetadataResolver()
                        .resolveDependencyQualifier(
                                annotatedElement,
                                componentQualifier,
                                graphContext.errors()::add);
                Dependency paramDependency =
                        Dependency.from(paramQualifier, paramType);
                dependencies[i] = paramDependency;
            }
            Type type = method.getGenericReturnType();
            Dependency dependency = Dependency.from(
                    graphConfig.getConfigurableMetadataResolver()
                            .resolve(method, graphContext.errors())
                            .merge(componentQualifier),
                    type);
            DependencyService dependencyService = graphCache.get(dependency);
            if (dependencyService == null) {
                Module module = componentInterface.getAnnotation(Module.class);
                ResourceMetadata<Method> resourceMetadata =
                        graphConfig.getConfigurableMetadataResolver()
                                .resolveMetadata(
                                        method,
                                        new ModuleMetadata(
                                                componentInterface,
                                                componentQualifier,
                                                module == null ? Module.NONE : module),
                                        graphContext.errors());
                internalProvider.getProvisionStrategy(
                        DependencyRequest.create(resourceMetadata, dependency));
                dependencyService = graphCache.get(dependency);
                throw new RuntimeException("not fucking here!"); // TODO
            } else {

            }
            configMap.put(method, new ComponentMethodConfig(dependencyService, dependencies));
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
                    ComponentMethodConfig config = configMap.get(method);
                    ResolutionContext resolutionContext = ResolutionContext.create();
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            resolutionContext.setParam(
                                    config.dependencies[i],
                                    args[i]);
                        }
                    }
                    return config.dependencyService
                            .force()
                            .get(internalProvider, resolutionContext);
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
