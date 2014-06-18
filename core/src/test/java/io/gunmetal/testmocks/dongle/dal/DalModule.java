package io.gunmetal.testmocks.dongle.dal;

import io.gunmetal.Module;
import io.gunmetal.testmocks.dongle.layers.Dal;
import io.gunmetal.testmocks.dongle.layers.Ui;

/**
 * @author rees.byars
 */
@Dal @Module(notAccessibleFrom = DalModule.BlackList.class)
public class DalModule {

    @Ui class BlackList implements io.gunmetal.BlackList { }

    public static DongleDao dongleDao() {
        return new DongleDao() { };
    }

}
