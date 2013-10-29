package io.gunmetal.builder;

import io.gunmetal.adapter.ModuleAdapter;

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
