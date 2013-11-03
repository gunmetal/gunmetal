package io.gunmetal.builder;

import java.util.List;

/**
 * @author rees.byars
 */
interface ModuleAdapter extends AccessFilter<ComponentAdapter<?>> {
    Class<?> getModuleClass();
    List<ComponentAdapter> getComponentAdapters();
    CompositeQualifier getCompositeQualifier();
}
