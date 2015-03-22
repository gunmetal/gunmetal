package io.gunmetal.sandbox.testmocks.dongle.ui;

import io.gunmetal.Provides;

/**
 * @author rees.byars
 */
interface FactoryTest {

    @Provides static FactoryTest create() {
        return new FactoryTest() {
        };
    }

}
