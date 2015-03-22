package io.gunmetal.sandbox.testmocks.dongle.bl;

import io.gunmetal.Ref;
import io.gunmetal.sandbox.testmocks.dongle.dal.DongleDao;

/**
 * @author rees.byars
 */
public class BlModule implements IBlModule {

    public DongleService dongleService(DongleDao dongleDao,
                                       DonglerFactory donglerFactory,
                                       Ref<RuntimeException> runtimeExceptionRef) {
        return new DongleService() {
        };
    }

}
