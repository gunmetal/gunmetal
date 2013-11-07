package io.gunmetal.builder;

/**
 * @author rees.byars
 */
interface InternalProvider {

    <T> ComponentAdapter<T> getComponentAdapter(DependencyRequest dependencyRequest);

    void register(Callback callback, BuildPhase phase);

    interface Callback { void call(); }

    enum BuildPhase {
        POST_WIRING, EAGER_INSTANTIATION
    }

}
