package com.github.overengineer.gunmetal.parameter;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

/**
 * @author rees.byars
 */
public class TrailingArgsParameterBuilder<T> implements ParameterBuilder<T> {

    private static final long serialVersionUID = -5628096865518677243L;
    private final ParameterProxy[] proxies;
    private final boolean decorator;

    TrailingArgsParameterBuilder(ParameterProxy[] proxies, boolean decorator) {
        this.proxies = proxies;
        this.decorator = decorator;
    }

    @Override
    public boolean isDecorator() {
        return decorator;
    }

    @Override
    public Object[] buildParameters(InternalProvider provider, ResolutionContext resolutionContext) {
        Object[] parameters = new Object[proxies.length];
        for (int i = 0; i < proxies.length; i++) {
            parameters[i] = proxies[i].get(provider, resolutionContext);
        }
        return parameters;
    }

    @Override
    public Object[] buildParameters(InternalProvider provider, ResolutionContext resolutionContext, Object[] trailingArgs) {
        Object[] parameters = new Object[proxies.length + trailingArgs.length];
        for (int i = 0; i < proxies.length; i++) {
            parameters[i] = proxies[i].get(provider, resolutionContext);
        }
        if (trailingArgs.length > 0) {
            if (proxies.length > 0) {
                System.arraycopy(trailingArgs, 0, parameters, proxies.length, trailingArgs.length + proxies.length - 1);
            } else {
                parameters = trailingArgs;
            }
        }
        return parameters;
    }
}
