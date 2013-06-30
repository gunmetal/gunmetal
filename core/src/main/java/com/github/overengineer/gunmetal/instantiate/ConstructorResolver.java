package com.github.overengineer.gunmetal.instantiate;

import com.github.overengineer.gunmetal.util.ConstructorRef;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ConstructorResolver extends Serializable {

    <T> ConstructorRef<T> resolveConstructor(Class<T> type, Class ... providedArgs);

}
