package io.gunmetal.builder;

/**
 * @author rees.byars
 */
interface ModuleBuilder {
    ModuleAdapter build(Class<?> module, InternalProvider internalProvider);
}
