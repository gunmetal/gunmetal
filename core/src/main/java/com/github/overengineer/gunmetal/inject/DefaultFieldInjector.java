package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.Provider;
import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.util.FieldRef;

/**
 * @author rees.byars
 */
public class DefaultFieldInjector<T> implements FieldInjector<T> {

    private final FieldRef fieldRef;
    private final Dependency<?> dependency;

    DefaultFieldInjector(FieldRef fieldRef, Dependency<?> dependency) {
        this.fieldRef = fieldRef;
        this.dependency = dependency;
    }

    @Override
    public void inject(T component, Provider provider) {
        try {
            fieldRef.getField().set(component, provider.get(dependency));
        } catch (Exception e) {
            throw new InjectionException("Could not inject field [" + fieldRef.getField().getName() + "] on component of type [" + component.getClass().getName() + "].", e);
        }
    }

}
