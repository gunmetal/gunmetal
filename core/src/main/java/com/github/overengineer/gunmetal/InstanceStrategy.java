package com.github.overengineer.gunmetal;

import com.github.overengineer.gunmetal.inject.ComponentInjector;

import java.util.List;

/**
 * @author rees.byars
 */
public class InstanceStrategy<T> implements ComponentStrategy<T> {

    private T instance;
    private final ComponentInjector<T> injector;
    private final Object qualifier;
    private final List<ComponentPostProcessor> postProcessors;
    private volatile boolean initialized = false;

    InstanceStrategy(T instance, ComponentInjector<T> injector, Object qualifier, List<ComponentPostProcessor> postProcessors) {
        this.instance = instance;
        this.injector = injector;
        this.qualifier = qualifier;
        this.postProcessors = postProcessors;
    }

    @Override
    public T get(InternalProvider provider, ResolutionContext resolutionContext) {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    injector.inject(instance, provider, resolutionContext);
                    for (ComponentPostProcessor postProcessor : postProcessors) {
                        instance = postProcessor.postProcess(instance);
                    }
                    initialized = true;
                }
            }
        }
        return instance;
    }

    @Override
    public Class getComponentType() {
        return instance.getClass();
    }

    @Override
    public boolean isDecorator() {
        return false;
    }

    @Override
    public Object getQualifier() {
        return qualifier;
    }
}
