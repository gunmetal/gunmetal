package com.github.overengineer.gunmetal.module;

import com.github.overengineer.gunmetal.key.Generic;

/**
 * @author rees.byars
 */
public class GenericMapping<T> extends TypeMapping<T> {

    @SuppressWarnings("unchecked")
    public GenericMapping(Generic<? extends T> generic) {
        super((Class<T>) generic.getTypeKey().getRaw());
    }

}
