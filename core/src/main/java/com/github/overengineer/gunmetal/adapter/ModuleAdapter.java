package com.github.overengineer.gunmetal.adapter;

import java.util.List;

/**
 * @author rees.byars
 */
public interface ModuleAdapter extends VisibilityAdapter<ModuleAdapter> {
    List<ComponentAdapter> getComponentAdapters();
}
