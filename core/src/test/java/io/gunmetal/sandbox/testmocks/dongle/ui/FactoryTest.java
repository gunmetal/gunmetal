package io.gunmetal.sandbox.testmocks.dongle.ui;

import io.gunmetal.Supplies;

/**
 * @author rees.byars
 */
interface FactoryTest {

    @Supplies static FactoryTest create() {
        return new FactoryTest() {
        };
    }

}
