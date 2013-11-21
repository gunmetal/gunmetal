package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface Instantiator<T> {

    T getInstance(InternalProvider provider, ResolutionContext resolutionContext);

    T getInstance(InternalProvider provider, ResolutionContext resolutionContext, Object... providedArgs);

}
