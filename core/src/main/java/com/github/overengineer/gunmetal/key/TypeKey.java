package com.github.overengineer.gunmetal.key;

import com.github.overengineer.gunmetal.util.TypeRef;

/**
 * @author rees.byars
 */
public interface TypeKey<T> extends TypeRef {
    Class<? super T> getRaw();
}
