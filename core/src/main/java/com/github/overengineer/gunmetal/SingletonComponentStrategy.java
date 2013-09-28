package com.github.overengineer.gunmetal;

/**
 * @author rees.byars
 */
public class SingletonComponentStrategy<T> implements ComponentStrategy<T> {

    private static final long serialVersionUID = 3253838484064171796L;
    private volatile T component;
    private final ComponentStrategy<T> delegateStrategy;

    SingletonComponentStrategy(ComponentStrategy<T> delegateStrategy) {
        this.delegateStrategy = delegateStrategy;
    }

    @Override
    public T get(InternalProvider provider, ResolutionContext resolutionContext) {
         if (component == null) {
             synchronized (this) {
                if (component == null) {
                    component = delegateStrategy.get(provider, resolutionContext);
                }
             }
         }
         return component;
    }

    @Override
    public Class getComponentType() {
        return delegateStrategy.getComponentType();
    }

    @Override
    public boolean isDecorator() {
        return delegateStrategy.isDecorator();
    }

    @Override
    public Object getQualifier() {
        return delegateStrategy.getQualifier();
    }

}
