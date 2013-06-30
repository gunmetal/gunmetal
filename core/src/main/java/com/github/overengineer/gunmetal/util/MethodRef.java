package com.github.overengineer.gunmetal.util;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
public interface MethodRef extends ParameterizedFunction {
    Method getMethod();
}
