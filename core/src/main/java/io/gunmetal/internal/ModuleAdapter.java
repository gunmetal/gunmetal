package io.gunmetal.internal;

import io.gunmetal.CompositeQualifier;

/**
 * @author rees.byars
 */
interface ModuleAdapter extends AccessFilter<DependencyRequest> {

    Class<?> getModuleClass();

    CompositeQualifier getCompositeQualifier();

    Class<?>[] getReferencedModules();

}
