package io.gunmetal.internal;

import io.gunmetal.Ref;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.ProvisionStrategy;

/**
 * @author rees.byars
 */
class RefStrategyFactory implements ReferenceStrategyFactory {

    @Override public ProvisionStrategy create(ProvisionStrategy provisionStrategy,
                                              DependencySupplier dependencySupplier,
                                              ComponentContext componentContext) {
        return createRefStrategy(provisionStrategy);
    }

    private static ProvisionStrategy createRefStrategy(ProvisionStrategy provisionStrategy) {
        return (p, c) -> new Ref<Object>() {
            volatile Object o;
            @Override public Object get() {
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
