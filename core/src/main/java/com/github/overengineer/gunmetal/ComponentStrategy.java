package com.github.overengineer.gunmetal;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ComponentStrategy<T> extends Serializable {

    T get(InternalProvider provider, ResolutionContext resolutionContext);

    Class getComponentType();

    boolean isDecorator();

    Object getQualifier();

}
