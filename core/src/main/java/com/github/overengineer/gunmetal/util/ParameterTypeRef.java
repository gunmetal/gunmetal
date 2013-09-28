package com.github.overengineer.gunmetal.util;

import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public class ParameterTypeRef implements TypeRef {

    private static final long serialVersionUID = 8750412550931356481L;
    private final ParameterizedFunction parameterizedFunction;
    private final int parameterIndex;
    private transient volatile Type type;

    public ParameterTypeRef(ParameterizedFunction parameterizedFunction, int parameterIndex) {
        this.parameterizedFunction = parameterizedFunction;
        this.parameterIndex = parameterIndex;
    }

    @Override
    public Type getType() {
        if (type == null) {
            synchronized (this) {
                if (type == null) {
                    type = parameterizedFunction.getParameterTypes()[parameterIndex];
                }
            }
        }
        return type;
    }

}
