package com.github.overengineer.gunmetal.builder;

import com.github.overengineer.gunmetal.adapter.ModuleAdapter;

/**
 * @author rees.byars
 */
public class ReflectiveModuleBuilder implements ModuleBuilder {

    private final ComponentAdapterFactory componentAdapterFactory;

    public ReflectiveModuleBuilder(ComponentAdapterFactory componentAdapterFactory) {
        this.componentAdapterFactory = componentAdapterFactory;
    }

    @Override
    public ModuleAdapter build(Class<?> module) {
        return null;
    }

}
