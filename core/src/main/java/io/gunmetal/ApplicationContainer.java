package io.gunmetal;

/**
 * @author rees.byars
 */
public interface ApplicationContainer {
    ApplicationContainer inject(Object injectionTarget);
    <T, D extends Dependency<T>> T get(Class<D> dependency);
}
