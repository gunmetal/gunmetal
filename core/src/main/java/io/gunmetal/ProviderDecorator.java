package io.gunmetal;

/**
 * @rees.byars
 */
public interface ProviderDecorator {
    <T> Provider<T> get(Dependency<T> dependency, Provider provider);
}
