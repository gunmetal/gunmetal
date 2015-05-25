package io.gunmetal.spi;

import java.util.function.Supplier;

/**
 * @author rees.byars
 */
public interface SupplierAdapter {

    boolean isSupplier(Dependency dependency);

    Object supplier(Supplier<?> supplier);

}
