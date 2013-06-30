package com.github.overengineer.container.module;

import com.github.overengineer.container.key.Generic;

/**
 * @author rees.byars
 */
public class GenericMapping<T> extends TypeMapping<T> {

    @SuppressWarnings("unchecked")
    public GenericMapping(Generic<? extends T> generic) {
        super((Class<T>) generic.getTypeKey().getRaw());
    }

}
