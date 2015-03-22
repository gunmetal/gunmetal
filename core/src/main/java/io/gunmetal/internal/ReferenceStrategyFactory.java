package io.gunmetal.internal;

import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProvisionStrategy;

/**
 * @author rees.byars
 */
interface ReferenceStrategyFactory {

    ProvisionStrategy create(ProvisionStrategy provisionStrategy, InternalProvider internalProvider);

}
