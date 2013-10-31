package io.gunmetal.builder;

import com.github.overengineer.gunmetal.InternalProvider;
import com.github.overengineer.gunmetal.ResolutionContext;

/**
 * @author rees.byars
 */
public class ProvisionStrategyProxy<T> implements ProvisionStrategy<T> {

    private ProvisionStrategy<T> provisionStrategy;

    ProvisionStrategyProxy() { }

    public void setProvisionStrategy(ProvisionStrategy<T> provisionStrategy) {
        this.provisionStrategy = provisionStrategy;
    }

    @Override
    public T get(InternalProvider provider, ResolutionContext resolutionContext) {
        if (provisionStrategy == null) {
            // TODO throw informative message
            throw new IllegalStateException("TODO!!!");
        }
        return provisionStrategy.get(provider, resolutionContext);
    }

}
