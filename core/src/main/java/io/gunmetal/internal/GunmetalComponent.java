package io.gunmetal.internal;

import io.gunmetal.Inject;
import io.gunmetal.MultiBind;
import io.gunmetal.Overrides;
import io.gunmetal.spi.ProvisionStrategyDecorator;

import java.util.Collections;
import java.util.List;

/**
 * @author rees.byars
 */
@Overrides(
        allowFieldInjection = true,
        allowImplicitModuleDependency = true)
final class GunmetalComponent {

    @Inject @MultiBind
    private List<ProvisionStrategyDecorator> strategyDecorators = Collections.emptyList();

    List<ProvisionStrategyDecorator> strategyDecorators() {
        return strategyDecorators;
    }

}
