package io.gunmetal.internal;

import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.SupplierAdapter;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResolutionContext;

/**
 * @author rees.byars
 */
class SupplierStrategyFactory implements ReferenceStrategyFactory {

    private static final ThreadLocal<ResolutionContext> contextThreadLocal = new ThreadLocal<>();
    private final SupplierAdapter supplierAdapter;

    SupplierStrategyFactory(SupplierAdapter supplierAdapter) {
        this.supplierAdapter = supplierAdapter;
    }

    public ProvisionStrategy create(
            final ProvisionStrategy provisionStrategy,
            final DependencySupplier dependencySupplier) {

        return (p, c) -> supplierAdapter.supplier(() -> {

            ResolutionContext context = contextThreadLocal.get();

            if (context != null) {
                return provisionStrategy.get(
                        dependencySupplier, context);
            }

            try {
                contextThreadLocal.set(ResolutionContext.create());
                return provisionStrategy.get(
                        dependencySupplier, contextThreadLocal.get());
            } finally {
                contextThreadLocal.remove();
            }

        });

    }

}
