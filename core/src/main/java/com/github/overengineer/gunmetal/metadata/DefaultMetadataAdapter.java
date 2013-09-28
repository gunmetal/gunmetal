package com.github.overengineer.gunmetal.metadata;

import com.github.overengineer.gunmetal.ComponentStrategy;
import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;
import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.key.Smithy;
import com.github.overengineer.gunmetal.scope.Scope;
import com.github.overengineer.gunmetal.scope.ScopedComponentStrategyProvider;
import com.github.overengineer.gunmetal.scope.Scopes;
import com.github.overengineer.gunmetal.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
public class DefaultMetadataAdapter implements MetadataAdapter {

    private static final long serialVersionUID = -2754376468103047870L;
    private final Map<Class<? extends Annotation>, Scope> scopes = new HashMap<Class<? extends Annotation>, Scope>();
    private final Map<Scope, ScopedComponentStrategyProvider> strategyProviders = new HashMap<Scope, ScopedComponentStrategyProvider>();

    {
        scopes.put(Prototype.class, Scopes.PROTOTYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetadataAdapter addScope(Scope scope, Class<? extends Annotation> scopeAnnotation, ScopedComponentStrategyProvider strategyProvider) {
        scopes.put(scopeAnnotation, scope);
        strategyProviders.put(scope, strategyProvider);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScopedComponentStrategyProvider getStrategyProvider(Scope scope) {
        return strategyProviders.get(scope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidConstructor(Constructor constructor) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scope getScope(Class cls) {
        for (Annotation annotation : cls.getAnnotations()) {
            Class annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(com.github.overengineer.gunmetal.metadata.Scope.class)) {
                for (Method method : annotationType.getMethods()) {
                    if (method.getName().equals("value") && Scope.class.isAssignableFrom(method.getReturnType())) {
                        try {
                            return (Scope) method.invoke(annotation);
                        } catch (Exception e) {
                            throw new MetadataException("There was an exception attempting to obtain the value of a qualifier", e);
                        }
                    }
                }
                return scopes.get(annotationType);
            }
        }
        return Scopes.UNDEFINED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scope getDefaultScope() {
        return Scopes.SINGLETON;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Method getInitMethod(Class<?> cls) {
        if (PostConstructable.class.isAssignableFrom(cls)) {
            try {
                return cls.getMethod("init");
            } catch (NoSuchMethodException e) {
                throw new MetadataException("An exception occurred obtaining init method from type [" + cls.getName() + "]", e);
            }
        }
        try {
            for (Method method : cls.getDeclaredMethods()) {
                if (method.getName().equals("postConstruct")) {
                    return method;
                }
            }
        } catch (Exception e) {
            throw new MetadataException("An exception occurred obtaining postConstruct method from type [" + cls.getName() + "]", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSetter(Method method) {
        return ReflectionUtil.isPublicSetter(method) && method.isAnnotationPresent(Inject.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldInject(Field field) {
        return field.isAnnotationPresent(Inject.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getQualifier(Type type, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            Class annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(com.github.overengineer.gunmetal.metadata.Qualifier.class)) {
                for (Method method : annotationType.getMethods()) {
                    if (method.getName().equals("value")) {
                        try {
                            return method.invoke(annotation);
                        } catch (Exception e) {
                            throw new MetadataException("There was an exception attempting to obtain the value of a qualifier", e);
                        }
                    }
                }
                return annotationType;
            }
        }
        return com.github.overengineer.gunmetal.key.Qualifier.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Method getCustomProviderMethod(Class<?> cls) {
        try {
            for (Method method : cls.getDeclaredMethods()) {
                if (method.getName().equals("get")) {
                    return method;
                }
            }
        } catch (Exception e) {
            throw new MetadataException("Could not obtain provider method named [get] from type [" + cls.getName() + "].  Make sure the class has a method named [get].", e);
        }
        throw new MetadataException("Could not obtain provider method named [get] from type [" + cls.getName() + "].  Make sure the class has a method named [get].", new IllegalArgumentException("There is no method named [get]"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dependency<?> getDelegateDependency(Method method) {
        if (!method.isAnnotationPresent(ImplementedBy.class)) {
            throw new MetadataException("There was an exception creating a delegated service", new IllegalArgumentException("The method [" + method.getName() + "] of class [" + method.getDeclaringClass() + "] must be annotated with an @" + ImplementedBy.class.getSimpleName() + " annotation"));
        }
        ImplementedBy delegate = method.getAnnotation(ImplementedBy.class);
        String name = delegate.name();
        if ("".equals(name)) {
            return Smithy.forge(delegate.value(), com.github.overengineer.gunmetal.key.Qualifier.NONE);
        }
        return Smithy.forge(delegate.value(), name);
    }

    @Override
    public Class<?> getProviderClass() {
        return Provider.class;
    }

    @Override
    public Object createProvider(final InternalProvider provider, final ComponentStrategy<?> providedTypeStrategy) {
        return new com.github.overengineer.gunmetal.metadata.Provider() {

            @Override
            public Object get() {
                return providedTypeStrategy.get(provider, ResolutionContext.Factory.create());
            }

        };
    }

}
