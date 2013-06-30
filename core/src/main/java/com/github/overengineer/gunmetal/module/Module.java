package com.github.overengineer.gunmetal.module;

import com.github.overengineer.gunmetal.key.Dependency;

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
