package io.gunmetal.builder;

import com.github.overengineer.gunmetal.key.Dependency;

/**
 * @author rees.byars
 */
interface InternalProvider {

    <T> ComponentAdapter<T> getComponentAdapter(Dependency<T> dependency);

    void register(Callback callback, BuildPhase phase);

    interface Callback { void call(); }

    enum BuildPhase {
        POST_WIRING, EAGER_INSTANTIATION
    }

}
