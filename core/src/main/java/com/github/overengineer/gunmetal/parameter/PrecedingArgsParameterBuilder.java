package com.github.overengineer.gunmetal.parameter;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

/**
 * @author rees.byars
 */
public class PrecedingArgsParameterBuilder<T> implements ParameterBuilder<T> {

    private final ParameterProxy[] proxies;
    private final boolean decorator;

    PrecedingArgsParameterBuilder(ParameterProxy[] proxies, boolean decorator) {
        this.proxies = proxies;
        this.decorator = decorator;
    }

    @Override
    public boolean isDecorator() {
        return decorator;
    }

    @Override
    public Object[] buildParameters(InternalProvider provider, ResolutionContext resolutionContext, Object[] precedingArgs) {
        Object[] parameters = new Object[proxies.length + precedingArgs.length];
        for (int i = precedingArgs.length; i < precedingArgs.length + proxies.length; i++) {
            parameters[i] = proxies[i - precedingArgs.length].get(provider, resolutionContext);
        }
        if (precedingArgs.length > 0) {
            if (proxies.length > 0) {
                System.arraycopy(precedingArgs, 0, parameters, 0, precedingArgs.length);
            } else {
                parameters = precedingArgs;
            }
        }
        return parameters;
    }
}
