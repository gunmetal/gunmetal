package io.gunmetal.spi.impl;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.SupplierAdapter;

import java.util.function.Supplier;

/**
 * @author rees.byars
 */
public final class Jsr330SupplierAdapter implements SupplierAdapter {

    @Override public boolean isSupplier(Dependency dependency) {
        return javax.inject.Provider.class.isAssignableFrom(dependency.typeKey().raw());
    }

    @Override public Object supplier(Supplier<?> supplier) {
        return (javax.inject.Provider) supplier::get;
    }

}
