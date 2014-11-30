package io.gunmetal.compiler;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/**
 * @author rees.byars
 */
interface AnnotationVisitor {

    void visit(AnnotationMirror annotationMirror, Element annotationElement);

}
