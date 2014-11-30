package io.gunmetal.compiler;

import javax.lang.model.element.Element;

/**
 * @author rees.byars
 */
final class MemberMetadata {

    private final Qualifier qualifier;
    private final Scope scope;
    private final Element element;

    MemberMetadata(
            Qualifier qualifier,
            Scope scope,
            Element element) {
        this.qualifier = qualifier;
        this.scope = scope;
        this.element = element;
    }

    Qualifier qualifier() {
        return qualifier;
    }

    Scope scope() {
        return scope;
    }

    Element element() {
        return element;
    }
}
