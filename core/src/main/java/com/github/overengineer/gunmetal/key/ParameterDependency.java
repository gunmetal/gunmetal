package com.github.overengineer.gunmetal.key;

import com.github.overengineer.gunmetal.util.TypeRef;
import com.github.overengineer.gunmetal.util.ReflectionUtil;

import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public class ParameterDependency<T> implements Dependency<T> {

    private static final long serialVersionUID = 6474512506734622330L;
    private final Object qualifier;
    private final TypeRef parameterRef;
    private final Class<? super T> targetClass;
    private final TypeKey<T> typeKey = new ParameterTypeKey();

    public ParameterDependency(TypeRef parameterRef, Object qualifier) {
        this.parameterRef = parameterRef;
        this.qualifier = qualifier;
        targetClass = ReflectionUtil.getRawClass(parameterRef.getType());
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

        private static final long serialVersionUID = 2189269158106746796L;

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
            return parameterRef.getType().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof TypeKey && getType().equals(((TypeKey) object).getType());
        }
    }

}
