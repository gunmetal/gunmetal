package com.github.overengineer.gunmetal.module;

import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.scope.Scope;

import java.io.Serializable;
import java.util.List;

/**
 * @author rees.byars
 */
public interface Mapping<T> extends Serializable {
    Class<T> getImplementationType();
    List<Class<?>> getTargetClasses();
    List<Dependency> getTargetDependencies();
    Scope getScope();
    Object getQualifier();
}
