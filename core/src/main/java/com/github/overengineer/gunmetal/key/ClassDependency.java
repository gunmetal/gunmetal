package com.github.overengineer.gunmetal.key;

import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public class ClassDependency<T> implements Dependency<T> {

    private final Object qualifier;
    private final TypeKey<T> typeKey;

    public ClassDependency(Class<T> targetClass) {
        qualifier = Qualifier.NONE;
        typeKey = new ClassTypeKey<T>(targetClass);
    }

    public ClassDependency(Class<T> targetClass, Object qualifier) {
        this.qualifier = qualifier == null ? Qualifier.NONE : qualifier;
        typeKey = new ClassTypeKey<T>(targetClass);
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

    static class ClassTypeKey<T> implements TypeKey<T> {

        private final Class<T> targetClass;

        ClassTypeKey(Class<T> targetClass) {
            this.targetClass = targetClass;
        }

        @Override
        public Type getType() {
            return targetClass;
        }

        @Override
        public Class<? super T> getRaw() {
            return targetClass;
        }

        @Override
        public int hashCode() {
            return targetClass.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof TypeKey && targetClass == ((TypeKey) object).getType();
        }

    }

}
