package com.github.overengineer.gunmetal.key;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface Dependency<T> extends Serializable {
    Object getQualifier();
    TypeKey<T> getTypeKey();
}
