package com.github.overengineer.gunmetal.metadata;

import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.scope.Scope;
import com.github.overengineer.gunmetal.scope.ScopedComponentStrategyProvider;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public interface MetadataAdapter extends Serializable {

    MetadataAdapter addScope(Scope scope, Class<? extends Annotation> scopeAnnotation, ScopedComponentStrategyProvider strategyProvider);

    ScopedComponentStrategyProvider getStrategyProvider(Scope scope);

    boolean isValidConstructor(Constructor constructor);

    Scope getScope(Class cls);

    Scope getDefaultScope();

    Method getInitMethod(Class<?> cls);

    boolean isSetter(Method method);

    boolean shouldInject(Field field);

    Object getQualifier(Type type, Annotation[] annotations);

    Method getCustomProviderMethod(Class<?> cls);

    Dependency<?> getDelegateDependency(Method method);

    Class<?> getProviderClass();

}
