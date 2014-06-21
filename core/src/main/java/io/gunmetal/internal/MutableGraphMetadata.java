package io.gunmetal.internal;

import io.gunmetal.Inject;
import io.gunmetal.Lazy;
import io.gunmetal.Qualifier;

import java.lang.annotation.Annotation;

/**
 * @author rees.byars
 */
final class MutableGraphMetadata {

    private boolean requireQualifiers = false;
    private boolean restrictPluralQualifiers = false;
    private boolean requireInterfaces = false;
    private boolean requireAcyclic = false;
    private boolean requireExplicitModuleDependencies = false;
    private boolean restrictFieldInjection = false;
    private boolean restrictSetterInjection = false;
    private Class<? extends Annotation> injectAnnotation = Inject.class;
    private Class<? extends Annotation> qualifierAnnotation = Qualifier.class;
    private Class<? extends Annotation> eagerAnnotation = Lazy.class;
    private boolean indicatesEager = false;
    
    MutableGraphMetadata() { }

    boolean isRequireQualifiers() {
        return requireQualifiers;
    }

    void setRequireQualifiers(boolean requireQualifiers) {
        this.requireQualifiers = requireQualifiers;
    }

    boolean isRestrictPluralQualifiers() {
        return restrictPluralQualifiers;
    }

    void setRestrictPluralQualifiers(boolean restrictPluralQualifiers) {
        this.restrictPluralQualifiers = restrictPluralQualifiers;
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

    Class<? extends Annotation> getInjectAnnotation() {
        return injectAnnotation;
    }

    void setInjectAnnotation(Class<? extends Annotation> injectAnnotation) {
        this.injectAnnotation = injectAnnotation;
    }

    Class<? extends Annotation> getQualifierAnnotation() {
        return qualifierAnnotation;
    }

    void setQualifierAnnotation(Class<? extends Annotation> qualifierAnnotation) {
        this.qualifierAnnotation = qualifierAnnotation;
    }

    Class<? extends Annotation> getEagerAnnotation() {
        return eagerAnnotation;
    }

    void setEagerAnnotation(Class<? extends Annotation> eagerAnnotation) {
        this.eagerAnnotation = eagerAnnotation;
    }

    boolean isIndicatesEager() {
        return indicatesEager;
    }

    void setIndicatesEager(boolean indicatesEager) {
        this.indicatesEager = indicatesEager;
    }

}
