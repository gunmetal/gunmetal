package io.gunmetal.builder;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

/**
 * @author rees.byars
 */
public interface ProvisionStrategy<T> {
    T get(InternalProvider provider, ResolutionContext resolutionContext);
}
