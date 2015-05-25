package io.gunmetal.sandbox.testmocks.dongle.ui;

import io.gunmetal.Inject;
import io.gunmetal.Overrides;
import io.gunmetal.sandbox.testmocks.dongle.bl.DongleService;
import io.gunmetal.sandbox.testmocks.dongle.layers.Bl;

/**
 * @author rees.byars
 */
public class DongleController {

    @Inject @Bl @Overrides(allowFieldInjection = true) DongleService dongleService;

}
