package com.github.overengineer.gunmetal.util;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public interface ParameterRef extends Serializable {
    Type getType();
}
