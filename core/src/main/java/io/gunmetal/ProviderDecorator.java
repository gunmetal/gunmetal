package io.gunmetal;

/**
 * @rees.byars
 */
public interface ProviderDecorator {
    <T> Provider<T> decorate(Object hashKey, Provider<T> provider);
}
