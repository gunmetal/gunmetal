package com.github.overengineer.gunmetal.builder;

import com.github.overengineer.gunmetal.adapter.ComponentAdapter;
import com.github.overengineer.gunmetal.adapter.ModuleAdapter;
import com.github.overengineer.gunmetal.adapter.QualifierAdapter;
import com.github.overengineer.gunmetal.scope.Scope;

/**
 * @author rees.byars
 */
public interface ComponentAdapterFactory {

    <T> ComponentAdapter<T> createFromClass(Class<T> implementationType, ModuleAdapter moduleAdapter, QualifierAdapter qualifierAdapter, Scope scope);

    <T> ComponentAdapter<T> createFromProviderMethod(T implementation, ModuleAdapter moduleAdapter, QualifierAdapter qualifierAdapter);

}
