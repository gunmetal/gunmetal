package io.gunmetal.internal;

/**
 * @author rees.byars
 */
final class MutableGraphMetadata {

    private boolean requireInterfaces = false;
    private boolean requireAcyclic = false;
    private boolean requireExplicitModuleDependencies = false;
    private boolean restrictFieldInjection = false;
    private boolean restrictSetterInjection = false;

    MutableGraphMetadata replicate() {
        MutableGraphMetadata copy = new MutableGraphMetadata();
        copy.requireInterfaces = requireInterfaces;
        copy.requireAcyclic = requireAcyclic;
        copy.requireExplicitModuleDependencies = requireExplicitModuleDependencies;
        copy.restrictFieldInjection = restrictFieldInjection;
        copy.restrictSetterInjection = restrictSetterInjection;
        return copy;
    }

    boolean isRequireInterfaces() {
        return requireInterfaces;
    }

    void setRequireInterfaces(boolean requireInterfaces) {
        this.requireInterfaces = requireInterfaces;
    }

    boolean isRequireAcyclic() {
        return requireAcyclic;
    }

    void setRequireAcyclic(boolean requireAcyclic) {
        this.requireAcyclic = requireAcyclic;
    }

    boolean isRequireExplicitModuleDependencies() {
        return requireExplicitModuleDependencies;
    }

    void setRequireExplicitModuleDependencies(boolean requireExplicitModuleDependencies) {
        this.requireExplicitModuleDependencies = requireExplicitModuleDependencies;
    }

    boolean isRestrictFieldInjection() {
        return restrictFieldInjection;
    }

    void setRestrictFieldInjection(boolean restrictFieldInjection) {
        this.restrictFieldInjection = restrictFieldInjection;
    }

    boolean isRestrictSetterInjection() {
        return restrictSetterInjection;
    }

    void setRestrictSetterInjection(boolean restrictSetterInjection) {
        this.restrictSetterInjection = restrictSetterInjection;
    }

}
