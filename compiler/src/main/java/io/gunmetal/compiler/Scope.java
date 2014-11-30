package io.gunmetal.compiler;

import io.gunmetal.Module;
import io.gunmetal.Singleton;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;

/**
 * @author rees.byars
 */
public interface Scope {

    enum Defaults implements Scope {

        PROTOTYPE,

        SINGLETON

    }

    static class Builder {

        Builder() { }

        private AnnotationMirror scopeMirror = null;

        Builder handleIfScope(AnnotationMirror annotationMirror, Element annotationElement) {
            if (Utils.isAnnotationPresent(annotationElement, io.gunmetal.Scope.class)) {
                if (scopeMirror != null) {
                    throw new IllegalArgumentException("More than one scope defined on "
                            + annotationElement.getEnclosingElement());
                }
                scopeMirror = annotationMirror;
            }
            return this;
        }

        Scope build() {

            if (scopeMirror == null) {
                return Defaults.PROTOTYPE;
            }

            DeclaredType declaredType = scopeMirror.getAnnotationType();
            // TODO is string comparison reliable?  probably not
            if (declaredType.toString().equals(Singleton.class.getName())) {
                return Defaults.SINGLETON;
            } else if (declaredType.toString().equals(Module.class.getName())) {
                return Defaults.SINGLETON;
            }

            return Defaults.PROTOTYPE;

        }

    }

}
