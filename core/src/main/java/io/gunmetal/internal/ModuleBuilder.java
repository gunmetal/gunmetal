package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface ModuleBuilder {
    void addBindings(Class<?> module, InternalProvider internalProvider, Bindings bindings);
}
