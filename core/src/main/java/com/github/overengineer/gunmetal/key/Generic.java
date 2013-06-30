package com.github.overengineer.gunmetal.key;

import com.github.overengineer.gunmetal.util.ReflectionUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 *
 * @author rees.byars
 */
public abstract class Generic<T> implements Dependency<T> {

    private final Object qualifier;
    private transient Type type;
    private transient Class<? super T> targetClass;
    private final int typeKeyHash;
    private final TypeKey<T> typeKey = new GenericTypeKey();

    public Generic() {
        init();
        this.qualifier = Qualifier.NONE;
        this.typeKeyHash = type.hashCode();
    }

    public Generic(Object qualifier) {
        init();
        this.qualifier = qualifier;
        this.typeKeyHash = type.hashCode();
    }

    private void init() {
        ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        type = parameterizedType.getActualTypeArguments()[0];
        if (type instanceof ParameterizedType) {
            targetClass = ReflectionUtil.getRawClass(type);
        } else {
            throw new UnsupportedOperationException("The GenericKey is invalid [" + type + "]");
        }
    }

    @Override
    public TypeKey<T> getTypeKey() {
        return typeKey;
    }

    @Override
    public Object getQualifier() {
        return qualifier;
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

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.init();
    }

    class GenericTypeKey implements TypeKey<T> {

        @Override
        public Type getType() {
            return type;
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
            return object instanceof TypeKey && type.equals(((TypeKey) object).getType());
        }
    }

}
