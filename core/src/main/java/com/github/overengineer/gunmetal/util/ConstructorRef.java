package com.github.overengineer.gunmetal.util;

import java.io.Serializable;
import java.lang.reflect.Constructor;

/**
 * @author rees.byars
 */
public interface ConstructorRef<T> extends Serializable {
    Constructor<T> getConstructor();
}
