package io.gunmetal.compiler;

/**
 * @author rees.byars
 */
final class ProviderLocation {

    private final MemberMetadata memberMetadata;

    ProviderLocation(
            MemberMetadata memberMetadata) {
        this.memberMetadata = memberMetadata;
    }

    MemberMetadata metadata() {
        return memberMetadata;
    }

}
