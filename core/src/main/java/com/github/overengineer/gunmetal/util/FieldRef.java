package com.github.overengineer.gunmetal.util;

import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * @author rees.byars
 */
public interface FieldRef extends Serializable {
    Field getField();
}
