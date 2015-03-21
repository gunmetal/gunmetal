package io.gunmetal.internal;

import io.gunmetal.Ref;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.util.Generics;

/**
 * @author rees.byars
 */
class RefStrategyFactory implements ReferenceStrategyFactory {

    @Override public <T> ProvisionStrategy<T> create(ProvisionStrategy<?> provisionStrategy,
                                                     InternalProvider internalProvider) {
        return Generics.as(createRefStrategy(provisionStrategy));
    }

    private static <T> ProvisionStrategy<Ref<T>> createRefStrategy(ProvisionStrategy<T> provisionStrategy) {
        return (p, c) -> new Ref<T>() {
            volatile T o;
            @Override public T get() {
                if (o == null) {
                    synchronized (this) {
                        if (o == null) {
                            o = provisionStrategy.get(p, c);
                        }
                    }
                }
                return o;
            }
        };
    }

}
