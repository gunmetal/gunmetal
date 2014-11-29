package io.gunmetal;

import javax.lang.model.element.Element;

/**
 * @author rees.byars
 */
class ProviderLocation implements GraphMember {

    private final MemberMetadata memberMetadata;
    private final Module moduleAnnotation;

    ProviderLocation(
            MemberMetadata memberMetadata,
            Module moduleAnnotation) {
        this.memberMetadata = memberMetadata;
        this.moduleAnnotation = moduleAnnotation;
    }

    static ProviderLocation fromElement(Element providerElement) {

        Element classElement = providerElement.getEnclosingElement();

        return new ProviderLocation(
                MemberMetadata.fromElement(classElement),
                classElement.getAnnotation(Module.class));

    }

    @Override public MemberMetadata metadata() {
        return memberMetadata;
    }

    Module moduleAnnotation() {
        return moduleAnnotation;
    }

}
