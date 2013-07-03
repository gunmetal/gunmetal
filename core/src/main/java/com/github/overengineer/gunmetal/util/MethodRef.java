package com.github.overengineer.gunmetal.util;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
public interface MethodRef extends Serializable {
    Method getMethod();
}
