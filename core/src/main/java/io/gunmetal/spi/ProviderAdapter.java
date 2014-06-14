package io.gunmetal.spi;

import io.gunmetal.Provider;

/**
 * @author rees.byars
 */
public interface ProviderAdapter {

    boolean isProvider(Dependency<?> dependency);

    Object provider(Provider<?> provider);

}
