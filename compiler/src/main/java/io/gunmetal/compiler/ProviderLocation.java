package io.gunmetal.compiler;

import io.gunmetal.Module;

/**
 * @author rees.byars
 */
final class ProviderLocation {

    private final MemberMetadata memberMetadata;
    private final Module moduleAnnotation;

    ProviderLocation(
            MemberMetadata memberMetadata,
            Module moduleAnnotation) {
        this.memberMetadata = memberMetadata;
        this.moduleAnnotation = moduleAnnotation;
    }

    MemberMetadata metadata() {
        return memberMetadata;
    }

    Module moduleAnnotation() {
        return moduleAnnotation;
    }

}
