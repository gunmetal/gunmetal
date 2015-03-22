package io.gunmetal.internal;

import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ProvisionStrategy;

/**
 * @author rees.byars
 */
interface DependencyService extends Replicable<DependencyService> {

    Binding binding();

    DependencyResponse service(DependencyRequest dependencyRequest, Errors errors);

    ProvisionStrategy force();

}
