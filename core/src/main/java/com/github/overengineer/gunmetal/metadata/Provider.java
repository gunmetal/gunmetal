package com.github.overengineer.gunmetal.metadata;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface Provider<T> extends Serializable {
    T get();
}
