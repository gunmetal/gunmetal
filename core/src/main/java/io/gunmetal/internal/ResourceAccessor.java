package io.gunmetal.internal;

import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ProvisionStrategy;

/**
 * @author rees.byars
 */
interface ResourceAccessor extends Replicable<ResourceAccessor> {

    Binding binding();

    ProvisionStrategy process(
            DependencyRequest dependencyRequest,
            Errors errors);

    ProvisionStrategy force();

}
