package io.gunmetal.builder;

import com.github.overengineer.gunmetal.ResolutionContext;

/**
 * @author rees.byars
 */
interface ProvisionStrategy<T> {
    T get(InternalProvider internalProvider, ResolutionContext resolutionContext);
}
