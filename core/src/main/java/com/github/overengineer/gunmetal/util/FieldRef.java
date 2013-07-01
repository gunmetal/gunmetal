package com.github.overengineer.gunmetal.util;

import java.lang.reflect.Field;

/**
 * @author rees.byars
 */
public interface FieldRef extends TypeRef {
    Field getField();
}
