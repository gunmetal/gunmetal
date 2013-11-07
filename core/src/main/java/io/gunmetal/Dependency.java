package io.gunmetal;

import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public interface Dependency<T> {

    CompositeQualifier getQualifier();

    Type getType();

    Class<? super T> getRaw();

    final class Factory {

        private Factory() { }

        public static <T> Dependency<T> create(Class<? super T> raw, CompositeQualifier compositeQualifier) {
            return null;
        }

        public static <T> Dependency<T> create(Type type, CompositeQualifier compositeQualifier) {
            return null;
        }

    }

}
