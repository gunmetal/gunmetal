package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface CollectionDependencyService extends DependencyService {

    void add(DependencyService dependencyService);

}
