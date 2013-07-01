package com.github.overengineer.gunmetal.key;

import com.github.overengineer.gunmetal.util.TypeRef;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public abstract class Smithy {

    private Smithy() { }

    public static <T> Dependency<T> forge(Class<T> cls) {
        return new ClassDependency<T>(cls);
    }

    public static <T> Dependency<T> forge(Class<T> cls, Object qualifier) {
        return new ClassDependency<T>(cls, qualifier);
    }

    public static <T> Dependency<T> forge(TypeRef parameterRef) {
        return forge(parameterRef, null);
    }

    @SuppressWarnings("unchecked")
    public static  <T> Dependency<T> forge(TypeRef parameterRef, Object qualifier) {
        Type type = parameterRef.getType();
        if (type instanceof Class) {
            return forge((Class<T>) type, qualifier);
        }
        if (type instanceof ParameterizedType) {
            return new ParameterDependency<T>(parameterRef, qualifier);
        }
        throw new UnsupportedOperationException("Unsupported injection type [" + type + "]");
    }


}
