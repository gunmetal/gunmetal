package io.gunmetal.builder;

import io.gunmetal.adapter.ModuleAdapter;

/**
 * @author rees.byars
 */
public interface ModuleBuilder {
    ModuleAdapter build(Class<?> module);
}
