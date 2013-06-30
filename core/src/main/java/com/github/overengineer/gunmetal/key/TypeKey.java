package com.github.overengineer.gunmetal.key;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public interface TypeKey<T> extends Serializable {
    Type getType();
    Class<? super T> getRaw();
}
