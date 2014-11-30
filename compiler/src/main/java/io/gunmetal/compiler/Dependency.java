package io.gunmetal.compiler;

import javax.lang.model.type.TypeMirror;

/**
 * @author rees.byars
 */
final class Dependency {

    private final TypeMirror typeMirror;
    private final Qualifier qualifier;
    private final int hashCode;

    Dependency(
            TypeMirror typeMirror,
            Qualifier qualifier) {
        this.typeMirror = typeMirror;
        this.qualifier = qualifier;
        hashCode = typeMirror().hashCode() * 67 + qualifier().hashCode();
    }

    TypeMirror typeMirror() {
        return typeMirror;
    }

    Qualifier qualifier() {
        return qualifier;
    }

    @Override public int hashCode() {
        return hashCode;
    }

    @Override public boolean equals(Object target) {
        if (target == this) {
            return true;
        }
        if (!(target instanceof Dependency)) {
            return false;
        }
        Dependency dependencyTarget = (Dependency) target;
        return dependencyTarget.qualifier().equals(qualifier())
                && dependencyTarget.typeMirror().equals(typeMirror());
    }

    @Override public String toString() {
        return "dependency[ " + qualifier().toString() + ", " + typeMirror().toString() + " ]";
    }

}
