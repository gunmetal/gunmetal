package io.gunmetal.compiler;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author rees.byars
 */
final class Qualifier {

    private final Object[] qualifiers;
    private final int hashCode;

    private Qualifier(Object[] qualifiers) {
        Arrays.sort(qualifiers);
        this.qualifiers = qualifiers;
        hashCode = Arrays.hashCode(qualifiers);
    }

    static class Builder {

        Builder() { }

        private final Set<Object> qualifiers = new HashSet<>();

        Builder addIfQualifier(AnnotationMirror annotationMirror, Element annotationElement) {
            if (Utils.isAnnotationPresent(annotationElement, io.gunmetal.Qualifier.class)) {
                // TODO are these Strings safe for comparison?  probably not...
                qualifiers.add(annotationMirror.toString());
            }
            return this;
        }

        Qualifier build() {
            return new Qualifier(qualifiers.toArray());
        }

    }

    Object[] qualifiers() {
        return qualifiers;
    }

    Qualifier merge(Qualifier other) {
        if (qualifiers.length == 0) {
            return other;
        }
        if (other.qualifiers().length == 0) {
            return this;
        }
        List<Object> qualifierList = new ArrayList<>();
        Collections.addAll(qualifierList, other.qualifiers());
        for (Object qualifier : qualifiers) {
            if (!qualifierList.contains(qualifier)) {
                qualifierList.add(qualifier);
            }
        }
        return new Qualifier(qualifierList.toArray());
    }

    boolean intersects(Object[] otherQualifiers) {
        for (Object qualifier : qualifiers) {
            for (Object otherQualifier : otherQualifiers) {
                if (qualifier.equals(otherQualifier)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean intersects(Qualifier qualifier) {
        return intersects(qualifier.qualifiers());
    }

    @Override public boolean equals(Object o) {
        return o instanceof Qualifier
                && (this == o || Arrays.equals(((Qualifier) o).qualifiers(), qualifiers));
    }

    @Override public int hashCode() {
        return hashCode;
    }

    @Override public String toString() {
        return "qualifier[ " + Arrays.toString(qualifiers()) + " ]";
    }

}
