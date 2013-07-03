package com.github.overengineer.gunmetal.inject;

import com.github.overengineer.gunmetal.CircularReferenceException;
import com.github.overengineer.gunmetal.Provider;
import com.github.overengineer.gunmetal.parameter.ParameterBuilder;
import com.github.overengineer.gunmetal.util.FieldProxy;
import com.github.overengineer.gunmetal.util.MethodProxy;

/**
 * @author rees.byars
 */
public class DefaultMethodInjector<T> implements MethodInjector<T> {

    private final MethodProxy methodProxy;
    private final ParameterBuilder parameterBuilder;

    DefaultMethodInjector(MethodProxy methodProxy, ParameterBuilder<T> parameterBuilder) {
        this.methodProxy = methodProxy;
        this.parameterBuilder = parameterBuilder;
    }

    @Override
    public Object inject(T component, Provider provider, Object ... providedArgs) {
        try {
            return methodProxy.invoke(component, parameterBuilder.buildParameters(provider, providedArgs));
        } catch (CircularReferenceException e) {

            //TODO this is dangerous - could result in unexpected behavior.  Log warning, ditch altogether??  It will work in nromal situations
            final FieldProxy fieldProxy = FieldProxy.Factory.create(methodProxy);

            if (fieldProxy != null) {
                e.addAccessor(new CircularReferenceException.TargetAccessor() {
                    @Override
                    public Object getTarget(Object reverseComponent) {
                        return fieldProxy.get(reverseComponent);
                    }
                });
            }

            throw e;
        }
    }

}
