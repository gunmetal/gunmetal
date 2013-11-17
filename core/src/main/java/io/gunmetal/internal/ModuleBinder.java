package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface ModuleBinder {
    void bind(Class<?> module, InternalProvider provider, Binder binder);
}
