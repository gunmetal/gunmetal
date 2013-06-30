package com.github.overengineer.container.module;

import com.github.overengineer.container.key.Dependency;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author rees.byars
 */
public interface Module extends Serializable {

    void configure();

    List<Mapping<?>> getMappings();

    Map<Dependency, Class> getNonManagedComponentFactories();

}
