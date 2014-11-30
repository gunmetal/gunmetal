package io.gunmetal.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author rees.byars
 */
final class Qualifier {

    private final Object[] qualifiers;
    private final int hashCode;

    Qualifier(Object[] qualifiers) {
        Arrays.sort(qualifiers);
        this.qualifiers = qualifiers;
        hashCode = Arrays.hashCode(qualifiers);
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

    String prettyName() {
        String prettyName = Arrays.toString(qualifiers());
        return prettyName.replaceAll("[^A-Za-z0-9]", "");
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
