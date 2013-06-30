package com.github.overengineer.gunmetal.util;

import java.lang.reflect.Constructor;

/**
 * @author rees.byars
 */
public interface ConstructorRef<T> extends ParameterizedFunction {
    Constructor<T> getConstructor();
}
