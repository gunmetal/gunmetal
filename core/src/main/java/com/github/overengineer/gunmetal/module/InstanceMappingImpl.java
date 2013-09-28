package com.github.overengineer.gunmetal.module;

/**
 * @author rees.byars
 */
public class InstanceMappingImpl<T> extends TypeMapping<T> implements InstanceMapping<T> {

    private static final long serialVersionUID = 8864612805295327892L;
    private final T instance;

    @SuppressWarnings("unchecked")
    public InstanceMappingImpl(T instance) {
        super((Class<T>) instance.getClass());
        this.instance = instance;
    }

    @Override
    public T getInstance() {
        return instance;
    }
}
