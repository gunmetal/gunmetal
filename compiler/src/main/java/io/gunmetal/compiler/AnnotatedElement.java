package io.gunmetal.compiler;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/**
 * @author rees.byars
 */
class AnnotatedElement {

    private final Element element;

    AnnotatedElement(
            Element element) {
        this.element = element;
    }

    void accept(AnnotationVisitor ... annotationVisitors) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            Element annotationElement = mirror.getAnnotationType().asElement();
            for (AnnotationVisitor visitor : annotationVisitors) {
                visitor.visit(mirror, annotationElement);
            }
        }
    }

}
