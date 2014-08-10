package io.gunmetal.testmocks.dongle.config;

import io.gunmetal.Module;
import io.gunmetal.Provides;
import io.gunmetal.spi.ProvisionStrategyDecorator;

import java.util.Collections;
import java.util.List;

/**
 * @author rees.byars
 */
@Module
public interface RootModule {

    @Provides static List<? extends ProvisionStrategyDecorator> decorators() {
        return Collections.emptyList();
    }

}
