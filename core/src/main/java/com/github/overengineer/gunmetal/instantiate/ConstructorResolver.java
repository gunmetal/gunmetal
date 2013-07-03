package com.github.overengineer.gunmetal.instantiate;

import com.github.overengineer.gunmetal.util.ConstructorProxy;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ConstructorResolver extends Serializable {

    <T> ConstructorProxy<T> resolveConstructor(Class<T> type, Class ... providedArgs);

}
