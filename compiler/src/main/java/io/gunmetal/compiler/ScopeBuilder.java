package io.gunmetal.compiler;

import io.gunmetal.Singleton;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;

/**
 * @author rees.byars
 */
class ScopeBuilder implements Builder<Scope> {

    private AnnotationMirror scopeMirror = null;

    @Override public void visit(AnnotationMirror annotationMirror, Element annotationElement) {
        if (Utils.isAnnotationPresent(annotationElement, io.gunmetal.Scope.class)) {
            if (scopeMirror != null) {
                throw new IllegalArgumentException("More than one scope defined on "
                        + annotationElement.getEnclosingElement());
            }
            scopeMirror = annotationMirror;
        }
    }

    @Override public Scope build() {

        if (scopeMirror == null) {
            return Scope.Defaults.PROTOTYPE;
        }

        DeclaredType declaredType = scopeMirror.getAnnotationType();
        // TODO is string comparison reliable?  probably not
        if (declaredType.toString().equals(Singleton.class.getName())) {
            return Scope.Defaults.SINGLETON;
        }

        return Scope.Defaults.PROTOTYPE;

    }

}
