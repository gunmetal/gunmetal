package com.github.overengineer.gunmetal.instantiate;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface InstantiatorFactory extends Serializable {
    <T> Instantiator<T> create(Class<T> implementationType, Class ... providedArgsTypes);
}
