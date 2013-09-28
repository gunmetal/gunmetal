package com.github.overengineer.gunmetal;

import com.github.overengineer.gunmetal.inject.MethodInjector;

/**
 * @author rees.byars
 */
public class CustomComponentStrategy<T> implements ComponentStrategy<T> {

    private static final long serialVersionUID = -395644679590133058L;
    private final ComponentStrategy<?> providerStrategy;
    private final MethodInjector methodInjector;
    private final Class providedType;
    private final Object qualifier;

    CustomComponentStrategy(ComponentStrategy providerStrategy, MethodInjector methodInjector, Class providedType, Object qualifier) {
        this.providerStrategy = providerStrategy;
        this.methodInjector = methodInjector;
        this.providedType = providedType;
        this.qualifier = qualifier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(InternalProvider provider, ResolutionContext resolutionContext) {
        return (T) methodInjector.inject(providerStrategy.get(provider, resolutionContext), provider, resolutionContext);
    }

    @Override
    public Class getComponentType() {
        return providedType;
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
