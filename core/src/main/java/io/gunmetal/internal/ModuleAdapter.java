package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface ModuleAdapter extends AccessFilter<DependencyRequest> {

    Class<?> moduleClass();

    Qualifier qualifier();

    Class<?>[] referencedModules();

}
