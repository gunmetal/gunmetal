package io.gunmetal.internal;

import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProvisionStrategy;

/**
 * @author rees.byars
 */
interface ReferenceStrategyFactory {

    <T> ProvisionStrategy<T> create(ProvisionStrategy<?> provisionStrategy, InternalProvider internalProvider);

}
