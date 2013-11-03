package io.gunmetal.builder;

import java.util.List;

/**
 * @author rees.byars
 */
interface ModuleAdapter extends VisibilityAdapter<ComponentAdapter<?>>, QualifierAdapter {
    Class<?> getModuleClass();
    List<ComponentAdapter> getComponentAdapters();
}
