package com.github.overengineer.container.key;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface Dependency<T> extends Serializable {
    Object getQualifier();
    TypeKey<T> getTypeKey();
}
