package com.github.overengineer.container.metadata;

import com.github.overengineer.container.scope.Scope;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
public class FastMetadataAdapter extends DefaultMetadataAdapter {

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

}
