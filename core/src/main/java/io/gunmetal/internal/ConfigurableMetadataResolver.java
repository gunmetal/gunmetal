package io.gunmetal.internal;

import io.gunmetal.AutoCollection;
import io.gunmetal.FromModule;
import io.gunmetal.Lazy;
import io.gunmetal.Overrides;
import io.gunmetal.Prototype;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.ComponentMetadataResolver;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.QualifierResolver;
import io.gunmetal.spi.Scope;
import io.gunmetal.spi.Scopes;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author rees.byars
 */
final class ConfigurableMetadataResolver implements ComponentMetadataResolver, QualifierResolver {

    private Class<? extends Annotation> qualifierType = io.gunmetal.Qualifier.class;
    private Class<? extends Annotation> eagerType = Lazy.class;
    private boolean indicatesEager = false;
    private Class<? extends Annotation> scopeType = io.gunmetal.Scope.class;
    private Map<Class<? extends Annotation>, Scope> scopeMap;
    private boolean requireQualifiers = false;
    private boolean restrictPluralQualifiers = false;

    ConfigurableMetadataResolver() {
        scopeMap = new HashMap<>();
        scopeMap.put(Prototype.class, Scopes.PROTOTYPE);
        scopeMap.put(null, Scopes.SINGLETON);
    }

    private ConfigurableMetadataResolver(Map<Class<? extends Annotation>, Scope> scopeMap) {
        this.scopeMap = new HashMap<>(scopeMap);
    }

    public ConfigurableMetadataResolver replicate() {
        ConfigurableMetadataResolver copy = new ConfigurableMetadataResolver(scopeMap);
        copy.qualifierType = qualifierType;
        copy.eagerType = eagerType;
        copy.indicatesEager = indicatesEager;
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

    @Override public ComponentMetadata<Method> resolveMetadata(Method method, ModuleMetadata moduleMetadata) {
        final Resolver resolver = new Resolver(method, moduleMetadata);
        return new ComponentMetadata<>(
                method,
                method.getDeclaringClass(),
                moduleMetadata,
                validate(resolver.qualifier),
                resolver.scope,
                resolver.overrides,
                resolver.eager,
                resolver.collectionElement);
    }

    @Override public ComponentMetadata<Class<?>> resolveMetadata(Class<?> cls, ModuleMetadata moduleMetadata) {
        final Resolver resolver = new Resolver(cls, moduleMetadata);
        return new ComponentMetadata<Class<?>>(
                cls,
                cls,
                moduleMetadata,
                validate(resolver.qualifier),
                resolver.scope,
                resolver.overrides,
                resolver.eager,
                resolver.collectionElement);
    }

    @Override public Qualifier resolve(AnnotatedElement annotatedElement) {
        return Qualifier.from(annotatedElement, qualifierType);
    }

    @Override public Qualifier resolveDependencyQualifier(AnnotatedElement parameter, Qualifier parentQualifier) {
        // TODO somewhat inefficient
        if (parameter.isAnnotationPresent(FromModule.class)) {
            return validate(Qualifier.from(parameter, qualifierType).merge(parentQualifier));
        }
        return validate(Qualifier.from(parameter, qualifierType));
    }

    private Qualifier validate(Qualifier qualifier) {
        // TODO should probably move to do for component only?
        // TODO dont count gunmetal qualifiers?
        if (restrictPluralQualifiers && qualifier.qualifiers().length > 1) {
            throw new IllegalArgumentException("Plural qualifiers restricted -> " + qualifier); // TODO
        }
        if (requireQualifiers && qualifier.qualifiers().length == 0) {
            throw new IllegalArgumentException("Qualifier required -> " + qualifier); // TODO
        }
        return qualifier;
    }

    private final class Resolver {

        Qualifier qualifier;
        Scope scope;
        Overrides overrides = Overrides.NONE;
        boolean collectionElement = false;
        boolean eager = !indicatesEager;

        Resolver(AnnotatedElement annotatedElement, ModuleMetadata moduleMetadata) {

            List<Object> qualifiers = new LinkedList<>();
            Class<? extends Annotation> scopeAnnotationType = null;
            for (Annotation annotation : annotatedElement.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (annotationType == Overrides.class) {
                    overrides = (Overrides) annotation;
                } else if (annotationType == AutoCollection.class) {
                    collectionElement = true;
                    qualifiers.add(annotation);
                } else if (annotationType == eagerType) {
                    eager = indicatesEager;
                }
                if (annotationType.isAnnotationPresent(scopeType)) {
                    scopeAnnotationType = annotationType;
                }
                if (annotationType.isAnnotationPresent(qualifierType)
                        && !qualifiers.contains(annotation)) {
                    qualifiers.add(annotation);
                }
            }

            if (qualifiers.isEmpty()) {
                qualifier = moduleMetadata.qualifier();
            } else {
                qualifier = Qualifier.from(qualifiers.toArray()).merge(moduleMetadata.qualifier());
            }

            scope = scopeMap.get(scopeAnnotationType);
            if (scope == null) {
                throw new UnsupportedOperationException("Scope is not mapped -> " + scopeAnnotationType); // TODO
            }

        }

    }

}
