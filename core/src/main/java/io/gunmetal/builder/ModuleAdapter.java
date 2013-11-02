package io.gunmetal.builder;

import java.util.List;

/**
 * @author rees.byars
 */
interface ModuleAdapter extends VisibilityAdapter<ModuleAdapter>, QualifierAdapter {
    List<ComponentAdapter> getComponentAdapters();
}
