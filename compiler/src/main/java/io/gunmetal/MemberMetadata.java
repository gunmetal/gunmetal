package io.gunmetal;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author rees.byars
 */
class MemberMetadata {

    // TODO abstract into types - Qualifier, Scope, etc
    private final Set<AnnotationMirror> qualifierMirrors;
    private final AnnotationMirror scopeMirror;

    MemberMetadata(
            Set<AnnotationMirror> qualifierMirrors,
            AnnotationMirror scopeMirror) {
        this.qualifierMirrors = qualifierMirrors;
        this.scopeMirror = scopeMirror;
    }

    static MemberMetadata fromElement(Element element) {

        Set<AnnotationMirror> qualifierMirrors = new HashSet<>();
        AnnotationMirror scopeMirror = null;

        List<? extends AnnotationMirror> annotationMirrors
                = element.getAnnotationMirrors();

        // Get provider scope, qualifier, etc - build out provider metadata
        for (AnnotationMirror mirror : annotationMirrors) {
            Element providerAnnotationElement = mirror.getAnnotationType().asElement();
            if (Utils.isAnnotationPresent(providerAnnotationElement, Qualifier.class)) {
                qualifierMirrors.add(mirror);
            }
            if (Utils.isAnnotationPresent(providerAnnotationElement, Scope.class)) {
                scopeMirror = mirror;
            }
            if (Utils.isAnnotationPresent(providerAnnotationElement, Overrides.class)) {
                // TODO
            }
            if (Utils.isAnnotationPresent(providerAnnotationElement, Lazy.class)) {
                // TODO
            }
            if (Utils.isAnnotationPresent(providerAnnotationElement, MultiBind.class)) {
                // TODO
            }
        }

        return new MemberMetadata(qualifierMirrors, scopeMirror);

    }

}
