package io.gunmetal.internal;

import io.gunmetal.Param;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResolutionContext;

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
final class ComponentGraph {

    private final ComponentConfig componentConfig;
    private final ComponentLinker componentLinker;
    private final DependencySupplier dependencySupplier;
    private final ComponentRepository componentRepository;
    private final ComponentContext componentContext;
    private final ComponentInjectors componentInjectors;
    private final Set<Class<?>> loadedModules;

    ComponentGraph(ComponentConfig componentConfig,
                   ComponentLinker componentLinker,
                   DependencySupplier dependencySupplier,
                   ComponentRepository componentRepository,
                   ComponentContext componentContext,
                   ComponentInjectors componentInjectors,
                   Set<Class<?>> loadedModules) {
        this.componentConfig = componentConfig;
        this.componentLinker = componentLinker;
        this.dependencySupplier = dependencySupplier;
        this.componentRepository = componentRepository;
        this.componentContext = componentContext;
        this.componentInjectors = componentInjectors;
        this.loadedModules = loadedModules;
    }

    void inject(Object injectionTarget) {
        componentInjectors
                .getInjector(injectionTarget, dependencySupplier, componentLinker, componentContext)
                .inject(injectionTarget, dependencySupplier, ResolutionContext.create());
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

    <T> T createProxy(Class<T> componentInterface) {

        Map<Method, ComponentMethodConfig> configMap = new HashMap<>();
        Qualifier componentQualifier = componentConfig
                .getConfigurableMetadataResolver()
                .resolve(componentInterface, componentContext.errors());
        for (Method method : componentInterface.getDeclaredMethods()) {
            // TODO complete these checks
            if (method.getReturnType() == void.class ||
                    method.getName().equals("plus")) {
                continue;
            }
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
                Qualifier paramQualifier = componentConfig
                        .getConfigurableMetadataResolver()
                        .resolveDependencyQualifier(
                                annotatedElement,
                                componentQualifier,
                                componentContext.errors()::add);
                Dependency paramDependency =
                        Dependency.from(paramQualifier, paramType);
                dependencies[i] = paramDependency;
            }
            Type type = method.getGenericReturnType();
            Dependency dependency = Dependency.from(
                    componentConfig.getConfigurableMetadataResolver()
                            .resolve(method, componentContext.errors())
                            .merge(componentQualifier),
                    type);
            DependencyService dependencyService = componentRepository.get(dependency);
            if (dependencyService == null) {
                throw new RuntimeException("not fucking here!"); // TODO
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
                    return config.dependencyService
                            .force()
                            .get(dependencySupplier, resolutionContext);
                }));
    }

    ComponentRepository graphCache() {
        return componentRepository;
    }

    Set<Class<?>> loadedModules() {
        return loadedModules;
    }

}
