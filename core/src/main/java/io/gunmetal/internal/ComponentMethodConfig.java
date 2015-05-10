package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.ProvisionStrategy;

/**
* @author rees.byars
*/
class ComponentMethodConfig {

    private final ProvisionStrategy provisionStrategy;
    private final Dependency[] dependencies;

    ComponentMethodConfig(ProvisionStrategy provisionStrategy,
                          Dependency[] dependencies) {
        this.provisionStrategy = provisionStrategy;
        this.dependencies = dependencies;
    }

    ProvisionStrategy provisionStrategy() {
        return provisionStrategy;
    }

    Dependency[] dependencies() {
        return dependencies;
    }

}
