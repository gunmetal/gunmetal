package com.github.overengineer.gunmetal.builder;

import com.github.overengineer.gunmetal.adapter.ModuleAdapter;

/**
 * @author rees.byars
 */
public interface ModuleBuilder {
    ModuleAdapter build(Class<?> module);
}
