package io.gunmetal;

/**
 * @rees.byars
 */
public interface ScopedProviderDecorator {
    <T> Provider<T> get(Dependency<? super T> dependency, Provider provider);
}
