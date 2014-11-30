package io.gunmetal.compiler;

import io.gunmetal.Lazy;
import io.gunmetal.MultiBind;
import io.gunmetal.Overrides;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/**
 * @author rees.byars
 */
class MemberMetadata {

    private final Qualifier qualifier;
    private final Scope scope;

    MemberMetadata(
            Qualifier qualifier,
            Scope scope) {
        this.qualifier = qualifier;
        this.scope = scope;
    }

    static MemberMetadata fromElement(Element element) {

        Qualifier.Builder qualifierBuilder = new Qualifier.Builder();
        Scope.Builder scopeBuilder = new Scope.Builder();

        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {

            Element annotationElement = mirror.getAnnotationType().asElement();

            qualifierBuilder.addIfQualifier(mirror, annotationElement);
            scopeBuilder.handleIfScope(mirror, annotationElement);

            if (Utils.isAnnotationPresent(annotationElement, Overrides.class)) {
                // TODO
            }
            if (Utils.isAnnotationPresent(annotationElement, Lazy.class)) {
                // TODO
            }
            if (Utils.isAnnotationPresent(annotationElement, MultiBind.class)) {
                // TODO
            }
        }

        return new MemberMetadata(qualifierBuilder.build(), scopeBuilder.build());

    }

    Qualifier qualifier() {
        return qualifier;
    }

    Scope scope() {
        return scope;
    }

}
