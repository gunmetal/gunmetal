package io.gunmetal.internal;

/**
 * @author rees.byars
 */
abstract class Dependency<T> {

    abstract Qualifier qualifier();

    abstract TypeKey<T> typeKey();

    public int hashCode() {
        return typeKey().hashCode() * 67 + qualifier().hashCode();
    }

    public boolean equals(Object target) {
        if (target == this) {
            return true;
        }
        if (!(target instanceof Dependency<?>)) {
            return false;
        }
        Dependency<?> dependencyTarget = (Dependency<?>) target;
        return dependencyTarget.qualifier().equals(qualifier())
                && dependencyTarget.typeKey().equals(typeKey());
    }

}
