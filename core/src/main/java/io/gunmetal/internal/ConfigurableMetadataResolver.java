package io.gunmetal.internal;

import io.gunmetal.FromModule;
import io.gunmetal.Lazy;
import io.gunmetal.Module;
import io.gunmetal.MultiBind;
import io.gunmetal.Overrides;
import io.gunmetal.Param;
import io.gunmetal.Singleton;
import io.gunmetal.Supplies;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionErrors;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.QualifierResolver;
import io.gunmetal.spi.ResourceMetadata;
import io.gunmetal.spi.ResourceMetadataResolver;
import io.gunmetal.spi.Scope;
import io.gunmetal.spi.Scopes;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rees.byars
 */
final class ConfigurableMetadataResolver implements ResourceMetadataResolver, QualifierResolver {

    private Class<? extends Annotation> qualifierType = io.gunmetal.Qualifier.class;
    private Class<? extends Annotation> eagerType = Lazy.class;
    private boolean indicatesEager = false;
    private Class<? extends Annotation> scopeType = io.gunmetal.Scope.class;
    private Map<Class<? extends Annotation>, Scope> scopeMap;
    private boolean requireQualifiers = false;
    private boolean restrictPluralQualifiers = false;

    ConfigurableMetadataResolver() {
        scopeMap = new HashMap<>();
        scopeMap.put(Singleton.class, Scopes.SINGLETON);
        scopeMap.put(null, Scopes.PROTOTYPE);
    }

    private ConfigurableMetadataResolver(Map<Class<? extends Annotation>, Scope> scopeMap) {
        this.scopeMap = new HashMap<>(scopeMap);
    }

    public ConfigurableMetadataResolver replicate() {
        ConfigurableMetadataResolver copy = new ConfigurableMetadataResolver(scopeMap);
        copy.qualifierType = qualifierType;
        copy.eagerType = eagerType;
        copy.indicatesEager = indicatesEager;
        copy.scopeType = scopeType;
        copy.requireQualifiers = requireQualifiers;
        copy.restrictPluralQualifiers = restrictPluralQualifiers;
        return copy;
    }

    public ConfigurableMetadataResolver qualifierType(Class<? extends Annotation> qualifierType) {
        this.qualifierType = qualifierType;
        return this;
    }

    public ConfigurableMetadataResolver scopeType(Class<? extends Annotation> scopeType) {
        this.scopeType = scopeType;
        return this;
    }

    public ConfigurableMetadataResolver addScope(Class<? extends Annotation> scopeType, Scope scope) {
        scopeMap.put(scopeType, scope);
        return this;
    }

    public ConfigurableMetadataResolver eagerType(Class<? extends Annotation> eagerType, boolean indicatesEager) {
        this.eagerType = eagerType;
        this.indicatesEager = indicatesEager;
        return this;
    }

    public ConfigurableMetadataResolver requireQualifiers(boolean requireQualifiers) {
        this.requireQualifiers = requireQualifiers;
        return this;
    }

    public ConfigurableMetadataResolver restrictPluralQualifiers(boolean restrictPluralQualifiers) {
        this.restrictPluralQualifiers = restrictPluralQualifiers;
        return this;
    }

    @Override public <T extends AnnotatedElement & Member> ResourceMetadata<T> resolveMetadata(
            T annotatedElement, ModuleMetadata moduleMetadata, Errors errors) {
        final Resolver resolver = new Resolver(annotatedElement, moduleMetadata);
        ResourceMetadata<T> resourceMetadata =
                new ResourceMetadata<>(
                        annotatedElement,
                        annotatedElement.getDeclaringClass(),
                        moduleMetadata,
                        resolver.qualifier(),
                        resolver.scope(),
                        resolver.overrides,
                        resolver.eager,
                        resolver.collectionElement,
                        resolver.isModule,
                        resolver.isProvider,
                        resolver.supplies,
                        resolver.isParam);
        validate(resourceMetadata, (error) -> errors.add(resourceMetadata, error));
        return resourceMetadata;
    }

    @Override public <T extends Parameter> ResourceMetadata<T> resolveMetadata(
            T annotatedElement, ModuleMetadata moduleMetadata, Errors errors) {
        final Resolver resolver = new Resolver(annotatedElement, moduleMetadata);
        ResourceMetadata<T> resourceMetadata =
                new ResourceMetadata<>(
                        annotatedElement,
                        annotatedElement.getDeclaringExecutable().getDeclaringClass(),
                        moduleMetadata,
                        resolver.qualifier(),
                        resolver.scope(),
                        resolver.overrides,
                        resolver.eager,
                        resolver.collectionElement,
                        resolver.isModule,
                        resolver.isProvider,
                        resolver.supplies,
                        resolver.isParam);
        validate(resourceMetadata, (error) -> errors.add(resourceMetadata, error));
        return resourceMetadata;
    }

    @Override public ResourceMetadata<Class<?>> resolveMetadata(Class<?> cls,
                                                                ModuleMetadata moduleMetadata,
                                                                Errors errors) {
        final Resolver resolver = new Resolver(cls, moduleMetadata);
        ResourceMetadata<Class<?>> resourceMetadata =
                new ResourceMetadata<>(
                        cls,
                        cls,
                        moduleMetadata,
                        resolver.qualifier(),
                        resolver.scope(),
                        resolver.overrides,
                        resolver.eager,
                        resolver.collectionElement,
                        resolver.isModule,
                        resolver.isProvider,
                        resolver.supplies,
                        resolver.isParam);
        validate(resourceMetadata, (error) -> errors.add(resourceMetadata, error));
        return resourceMetadata;
    }

    @Override public Qualifier resolve(AnnotatedElement annotatedElement) {
        return Qualifier.from(annotatedElement, qualifierType);
    }

    @Override public Qualifier resolveDependencyQualifier(AnnotatedElement parameter,
                                                          Qualifier parentQualifier) {
        // TODO somewhat inefficient
        if (parameter.isAnnotationPresent(FromModule.class)) {
            return Qualifier.from(parameter, qualifierType).merge(parentQualifier);
        }
        return Qualifier.from(parameter, qualifierType);
    }

    private void validate(ResourceMetadata<?> resourceMetadata, ProvisionErrors errors) {
        if (restrictPluralQualifiers
                && !resourceMetadata.overrides().allowPluralQualifier()
                && resourceMetadata.qualifier().qualifiers().length > 1
                && isPlural(resourceMetadata.qualifier())) {
            errors.add("Plural qualifiers restricted -> " + resourceMetadata.qualifier());
        }
        if (requireQualifiers
                && !resourceMetadata.overrides().allowNoQualifier()
                && resourceMetadata.qualifier().qualifiers().length == 0) {
            errors.add("Qualifier required -> " + resourceMetadata.qualifier());
        }
    }

    private boolean isPlural(Qualifier qualifier) {
        boolean foundQualifier = false;
        for (Object q : qualifier.qualifiers()) {
            if (q instanceof Annotation
                    && ((Annotation) q).annotationType().getPackage() != MultiBind.class.getPackage()) {
                if (foundQualifier) {
                    return true;
                } else {
                    foundQualifier = true;
                }
            }
        }
        return false;
    }

    private final class Resolver {

        final List<Object> qualifiers = new ArrayList<>();
        final ModuleMetadata moduleMetadata;
        Class<? extends Annotation> scopeAnnotationType = null;
        Overrides overrides = Overrides.NONE;
        boolean collectionElement = false;
        boolean eager = !indicatesEager;
        boolean isModule = false;
        boolean isProvider = false;
        Supplies supplies= Supplies.NONE;
        boolean isParam = false;

        Resolver(AnnotatedElement annotatedElement, ModuleMetadata moduleMetadata) {
            this.moduleMetadata = moduleMetadata;
            for (Annotation annotation : annotatedElement.getAnnotations()) {
                processAnnotation(annotation);
            }
        }

        Qualifier qualifier() {
            if (qualifiers.isEmpty()) {
                return moduleMetadata.qualifier();
            }
            return Qualifier.from(qualifiers.toArray()).merge(moduleMetadata.qualifier());
        }

        Scope scope() {
            Scope scope = scopeMap.get(scopeAnnotationType);
            if (scope == null) {
                // TODO message and look into adding to an errors instance
                throw new UnsupportedOperationException("Scope is not mapped -> " + scopeAnnotationType);
            }
            return scope;
        }

        private void processAnnotation(Annotation annotation) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType == Supplies.class) {
                isProvider = true;
                supplies = (Supplies) annotation;
            } else if (annotationType == Overrides.class) {
                overrides = (Overrides) annotation;
            } else if (annotationType == MultiBind.class) {
                collectionElement = true;
                qualifiers.add(annotation);
            } else if (annotationType == eagerType) {
                eager = indicatesEager;
            } else if (annotationType == Module.class) {
                isModule = true;
            } else if (annotationType == Param.class) {
                isParam = true;
                qualifiers.add(annotation);
            } else {
                if (annotationType.isAnnotationPresent(scopeType)) {
                    scopeAnnotationType = annotationType;
                }
                if (annotationType.isAnnotationPresent(qualifierType)
                        && !qualifiers.contains(annotation)) {
                    qualifiers.add(annotation);
                }
            }
        }

    }

}
