package io.gunmetal.spi.impl;

import io.gunmetal.Provider;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.ProviderAdapter;

/**
 * @author rees.byars
 */
public final class GunmetalProviderAdapter implements ProviderAdapter {

    @Override public boolean isProvider(Dependency<?> dependency) {
        return Provider.class.isAssignableFrom(dependency.typeKey().raw());
    }

    @Override public Object provider(Provider<?> provider) {
        return provider;
    }

}
