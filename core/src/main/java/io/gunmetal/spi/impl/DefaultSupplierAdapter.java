package io.gunmetal.spi.impl;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.SupplierAdapter;

import java.util.function.Supplier;

/**
 * @author rees.byars
 */
public final class DefaultSupplierAdapter implements SupplierAdapter {

    @Override public boolean isSupplier(Dependency dependency) {
        return Supplier.class.isAssignableFrom(dependency.typeKey().raw());
    }

    @Override public Object supplier(Supplier<?> supplier) {
        return supplier;
    }

}
