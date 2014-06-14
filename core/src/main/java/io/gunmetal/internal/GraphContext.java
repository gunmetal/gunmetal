package io.gunmetal.internal;

import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategyDecorator;

/**
 * @author rees.byars
 */
interface GraphContext {

    ProvisionStrategyDecorator strategyDecorator();

    Linkers linkers();

    static GraphContext create(final ProvisionStrategyDecorator strategyDecorator, final Linkers linkers) {
        return new GraphContext() {
            @Override public ProvisionStrategyDecorator strategyDecorator() {
                return strategyDecorator;
            }

            @Override public Linkers linkers() {
                return linkers;
            }
        };
    }

}
