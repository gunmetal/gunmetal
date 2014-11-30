package io.gunmetal.compiler;

import java.util.List;

/**
 * @author rees.byars
 */
final class Binding {

    private final MemberMetadata providerMetadata;
    private final ProviderKind kind;
    private final ProviderLocation location;
    private final Dependency fulfilledDependency;
    private final List<Dependency> requiredDependencies;

    Binding(
            MemberMetadata providerMetadata,
            ProviderKind kind,
            ProviderLocation location,
            Dependency fulfilledDependency,
            List<Dependency> requiredDependencies) {
        this.providerMetadata = providerMetadata;
        this.kind = kind;
        this.location = location;
        this.fulfilledDependency = fulfilledDependency;
        this.requiredDependencies = requiredDependencies;
    }

    MemberMetadata providerMetadata() {
        return providerMetadata;
    }

    ProviderKind kind() {
        return kind;
    }

    ProviderLocation location() {
        return location;
    }

    Dependency fulfilledDependency() {
        return fulfilledDependency;
    }

    List<Dependency> requiredDependencies() {
        return requiredDependencies;
    }

}
