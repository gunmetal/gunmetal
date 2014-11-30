package io.gunmetal.compiler;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.HashSet;
import java.util.Set;

/**
 * @author rees.byars
 */
class QualifierBuilder implements Builder<Qualifier> {

    private final Set<Object> qualifiers = new HashSet<>();

    @Override public void visit(AnnotationMirror annotationMirror, Element annotationElement) {
        if (Utils.isAnnotationPresent(annotationElement, io.gunmetal.Qualifier.class)) {
            // TODO are these Strings safe for comparison?  probably not...
            qualifiers.add(annotationMirror.toString());
        }
    }

    @Override public Qualifier build() {
        return new Qualifier(qualifiers.toArray());
    }

}
