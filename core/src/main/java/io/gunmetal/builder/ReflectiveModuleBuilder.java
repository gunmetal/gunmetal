package io.gunmetal.builder;

/**
 * @author rees.byars
 */
public class ReflectiveModuleBuilder implements ModuleBuilder {

    private final ComponentAdapterFactory componentAdapterFactory;

    public ReflectiveModuleBuilder(ComponentAdapterFactory componentAdapterFactory) {
        this.componentAdapterFactory = componentAdapterFactory;
    }

    @Override
    public ModuleAdapter build(Class<?> module, InternalProvider internalProvider) {
        return null;
    }

}
