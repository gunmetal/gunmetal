package io.gunmetal.internal;

import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.ProvisionStrategy;

/**
 * @author rees.byars
 */
interface ReferenceStrategyFactory {

    ProvisionStrategy create(ProvisionStrategy provisionStrategy, DependencySupplier dependencySupplier);

}
