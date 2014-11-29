package io.gunmetal;

import javax.lang.model.element.Element;

/**
 * @author rees.byars
 */
class Provider implements GraphMember {

    // TODO Dependency fulfills(), List<Dependency> requires()

    private final MemberMetadata memberMetadata;
    private final ProviderKind kind;
    private final ProviderLocation location;

    Provider(
            MemberMetadata memberMetadata,
            ProviderKind kind,
            ProviderLocation location) {
        this.memberMetadata = memberMetadata;
        this.kind = kind;
        this.location = location;
    }

    static Provider fromElement(Element providerElement) {

        return new Provider(
                MemberMetadata.fromElement(providerElement),
                ProviderKind.fromElement(providerElement),
                ProviderLocation.fromElement(providerElement));

    }

    @Override public MemberMetadata metadata() {
        return memberMetadata;
    }

    ProviderKind kind() {
        return kind;
    }

    ProviderLocation location() {
        return location;
    }

}
