package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;

/**
* @author rees.byars
*/
class ComponentMethodConfig {

    private final ResourceAccessor resourceAccessor;
    private final Dependency[] dependencies;

    ComponentMethodConfig(ResourceAccessor resourceAccessor,
                          Dependency[] dependencies) {
        this.resourceAccessor = resourceAccessor;
        this.dependencies = dependencies;
    }

    ResourceAccessor resourceAccessor() {
        return resourceAccessor;
    }

    Dependency[] dependencies() {
        return dependencies;
    }

}
