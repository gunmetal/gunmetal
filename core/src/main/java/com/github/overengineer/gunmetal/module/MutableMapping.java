package com.github.overengineer.gunmetal.module;

import com.github.overengineer.gunmetal.key.Generic;
import com.github.overengineer.gunmetal.scope.Scope;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface MutableMapping<T> extends Serializable {
    MutableMapping<T> forAllTypes();
    MutableMapping<T> forType(Class<? super T> targetClass);
    MutableMapping<T> forType(Generic<? super T> targetDependency);
    MutableMapping<T> withScope(Scope scope);
    MutableMapping<T> withQualifier(Object qualifier);
}
