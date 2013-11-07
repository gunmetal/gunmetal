package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface ProvisionStrategy<T> {
    T get(InternalProvider internalProvider, ResolutionContext resolutionContext);
}
