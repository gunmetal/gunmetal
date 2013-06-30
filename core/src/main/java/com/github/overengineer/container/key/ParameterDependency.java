package com.github.overengineer.container.key;

import com.github.overengineer.container.util.ParameterRef;
import com.github.overengineer.container.util.ReflectionUtil;

import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public class ParameterDependency<T> implements Dependency<T> {

    private final Object qualifier;
    private final ParameterRef parameterRef;
    private final Class<? super T> targetClass;
    private final int typeKeyHash;
    private final TypeKey<T> typeKey = new ParameterTypeKey();

    public ParameterDependency(ParameterRef parameterRef) {
        this.parameterRef = parameterRef;
        qualifier = Qualifier.NONE;
        Type type = parameterRef.getType();
        targetClass = ReflectionUtil.getRawClass(type);
        typeKeyHash = type.hashCode();
    }

    public ParameterDependency(ParameterRef parameterRef, Object qualifier) {
        this.parameterRef = parameterRef;
        this.qualifier = qualifier;
        Type type = parameterRef.getType();
        targetClass = ReflectionUtil.getRawClass(type);
        typeKeyHash = type.hashCode();
    }

    @Override
    public Object getQualifier() {
        return qualifier;
    }

    @Override
    public TypeKey<T> getTypeKey() {
        return typeKey;
    }

    @Override
    public int hashCode() {
        return typeKey.hashCode() * 31 + qualifier.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Dependency
                && qualifier.equals(((Dependency) object).getQualifier())
                && typeKey.equals(((Dependency) object).getTypeKey());
    }

    class ParameterTypeKey implements TypeKey<T> {

        @Override
        public Type getType() {
            return parameterRef.getType();
        }

        @Override
        public Class<? super T> getRaw() {
            return targetClass;
        }

        @Override
        public int hashCode() {
            return typeKeyHash;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof TypeKey && getType().equals(((TypeKey) object).getType());
        }
    }

}
