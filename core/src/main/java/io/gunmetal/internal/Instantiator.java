package io.gunmetal.internal;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

/**
 * @author rees.byars
 */
interface Instantiator<T> {

    T getInstance(InternalProvider provider, ResolutionContext resolutionContext);

    T getInstance(InternalProvider provider, ResolutionContext resolutionContext, Object... providedArgs);

}
