package io.gunmetal.internal;

import io.gunmetal.Provider;
import io.gunmetal.spi.Config;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.util.Generics;

/**
 * @author rees.byars
 */
class ProviderStrategyFactory {

    private final Config config;

    ProviderStrategyFactory(Config config) {
        this.config = config;
    }

    <T> ProvisionStrategy<T> create(final ProvisionStrategy<?> componentStrategy,
                                    final InternalProvider internalProvider) {

        final Object provider = config.provider(new Provider<Object>() {

            final ThreadLocal<ResolutionContext> contextThreadLocal = new ThreadLocal<>();

            @Override public Object get() {

                ResolutionContext context = contextThreadLocal.get();

                if (context != null) {
                    return componentStrategy.get(
                            internalProvider, context);
                }

                try {
                    context = ResolutionContext.create();
                    contextThreadLocal.set(context);
                    return componentStrategy.get(
                            internalProvider, context);
                } finally {
                    contextThreadLocal.remove();
                }

            }

        });

        return (p, c) -> Generics.as(provider);

    }

}
