package io.gunmetal;

import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public interface Dependency<T> {

    CompositeQualifier getQualifier();

    Type getType();

    Class<? super T> getRaw();

}
