package com.github.overengineer.gunmetal;

/**
 * @author rees.byars
 */
public class TopLevelStrategy<T> implements ComponentStrategy<T> {

    private static final long serialVersionUID = -2925894461059990202L;
    private final ComponentStrategy<T> delegateStrategy;

    TopLevelStrategy(ComponentStrategy<T> delegateStrategy) {
        this.delegateStrategy = delegateStrategy;
    }

    @Override
    public T get(InternalProvider provider, ResolutionContext resolutionContext) {
        return delegateStrategy.get(provider, resolutionContext);
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
