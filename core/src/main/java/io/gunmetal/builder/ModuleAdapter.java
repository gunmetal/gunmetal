package io.gunmetal.builder;

/**
 * @author rees.byars
 */
interface ModuleAdapter extends AccessFilter<DependencyRequest> {

    Class<?> getModuleClass();

    CompositeQualifier getCompositeQualifier();

    boolean dependsOn(Class<?> otherModule);

}
