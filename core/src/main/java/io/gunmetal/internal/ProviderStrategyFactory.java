package io.gunmetal.internal;

import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProviderAdapter;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.util.Generics;

/**
 * @author rees.byars
 */
class ProviderStrategyFactory implements ReferenceStrategyFactory {

    private static final ThreadLocal<ResolutionContext> contextThreadLocal = new ThreadLocal<>();
    private final ProviderAdapter providerAdapter;

    ProviderStrategyFactory(ProviderAdapter providerAdapter) {
        this.providerAdapter = providerAdapter;
    }

    public <T> ProvisionStrategy<T> create(
            final ProvisionStrategy<?> provisionStrategy,
            final InternalProvider internalProvider) {

        return (p, c) -> Generics.as(providerAdapter.provider(() -> {

            ResolutionContext context = contextThreadLocal.get();

            if (context != null) {
                return provisionStrategy.get(
                        internalProvider, context);
            }

            try {
                contextThreadLocal.set(ResolutionContext.create());
                return provisionStrategy.get(
                        internalProvider, contextThreadLocal.get());
            } finally {
                contextThreadLocal.remove();
            }

        }));

    }

}
