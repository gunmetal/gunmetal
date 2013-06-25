package com.github.overengineer.container.metadata;

import com.github.overengineer.container.key.Key;
import com.github.overengineer.container.key.Locksmith;
import com.github.overengineer.container.scope.Scope;
import com.github.overengineer.container.scope.ScopedComponentStrategyProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
public class FastMetadataAdapter implements MetadataAdapter {

    private final Map<Scope, ScopedComponentStrategyProvider> strategyProviders = new HashMap<Scope, ScopedComponentStrategyProvider>();

    /**
     * {@inheritDoc}
     */
    @Override
    public MetadataAdapter addScope(Scope scope, Class<? extends Annotation> scopeAnnotation, ScopedComponentStrategyProvider strategyProvider) {
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
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Method getInitMethod(Class<?> cls) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSetter(Method method) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getQualifier(Type type, Annotation[] annotations) {
        return com.github.overengineer.container.key.Qualifier.NONE;
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
    public Key<?> getDelegateKey(Method method) {
        if (!method.isAnnotationPresent(ImplementedBy.class)) {
            throw new MetadataException("There was an exception creating a delegated service", new IllegalArgumentException("The method [" + method.getName() + "] of class [" + method.getDeclaringClass() + "] must be annotated with an @Delegate annotation"));
        }
        ImplementedBy delegate = method.getAnnotation(ImplementedBy.class);
        String name = delegate.name();
        if ("".equals(name)) {
            return Locksmith.makeKey(delegate.value(), com.github.overengineer.container.key.Qualifier.NONE);
        }
        return Locksmith.makeKey(delegate.value(), name);
    }

    @Override
    public Class<?> getProviderClass() {
        return Provider.class;
    }

}
