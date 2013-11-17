package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface InternalProvider {

    <T> ProvisionStrategy<T> getProvisionStrategy(DependencyRequest dependencyRequest);

    void register(Callback callback, BuildPhase phase);

    enum BuildPhase {
        POST_WIRING, EAGER_INSTANTIATION
    }

}
