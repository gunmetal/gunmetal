package io.gunmetal.testmocks.dongle.bl;

import io.gunmetal.FromModule;
import io.gunmetal.Ref;
import io.gunmetal.testmocks.dongle.dal.DongleDao;
import io.gunmetal.testmocks.dongle.layers.Dal;

/**
 * @author rees.byars
 */
public class BlModule implements IBlModule {

    public DongleService dongleService(@Dal DongleDao dongleDao,
                                       @FromModule DonglerFactory donglerFactory,
                                       @FromModule Ref<RuntimeException> runtimeExceptionRef) {
        return new DongleService() { };
    }

}
