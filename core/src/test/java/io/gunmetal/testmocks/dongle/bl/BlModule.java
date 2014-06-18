package io.gunmetal.testmocks.dongle.bl;

import io.gunmetal.Module;
import io.gunmetal.testmocks.dongle.layers.Bl;
import io.gunmetal.testmocks.dongle.layers.Dal;
import io.gunmetal.testmocks.dongle.dal.DalModule;
import io.gunmetal.testmocks.dongle.dal.DongleDao;

/**
 * @author rees.byars
 */
@Bl @Module(dependsOn = DalModule.class, notAccessibleFrom = BlModule.BlackList.class)
public class BlModule {

    @Dal class BlackList implements io.gunmetal.BlackList { }

    public static DongleService dongleService(@Dal DongleDao dongleDao) {
        return new DongleService() { };
    }

}
