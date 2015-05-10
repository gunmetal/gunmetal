package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;

/**
* @author rees.byars
*/
class ComponentMethodConfig {

    private final DependencyRequest dependencyRequest;
    private final Dependency[] dependencies;

    ComponentMethodConfig(DependencyRequest dependencyRequest,
                          Dependency[] dependencies) {
        this.dependencyRequest = dependencyRequest;
        this.dependencies = dependencies;
    }

    DependencyRequest dependencyRequest() {
        return dependencyRequest;
    }

    Dependency[] dependencies() {
        return dependencies;
    }

}
