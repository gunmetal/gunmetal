package io.gunmetal.internal;

import io.gunmetal.MultiBind;
import io.gunmetal.spi.ProvisionStrategyDecorator;

import java.util.Collections;
import java.util.List;

/**
 * @author rees.byars
 */
final class GunmetalComponent {

    @MultiBind private List<ProvisionStrategyDecorator> strategyDecorators = Collections.emptyList();

    List<ProvisionStrategyDecorator> strategyDecorators() {
        return strategyDecorators;
    }

}
