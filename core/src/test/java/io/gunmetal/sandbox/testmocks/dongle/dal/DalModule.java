package io.gunmetal.sandbox.testmocks.dongle.dal;

import io.gunmetal.Inject;
import io.gunmetal.Module;
import io.gunmetal.Overrides;
import io.gunmetal.Provides;
import io.gunmetal.sandbox.testmocks.dongle.layers.Bl;
import io.gunmetal.sandbox.testmocks.dongle.layers.Dal;
import io.gunmetal.sandbox.testmocks.dongle.layers.Ui;

/**
 * @author rees.byars
 */
@Overrides(allowPluralQualifier = true, allowCycle = true)
@Dal
@Module(notAccessibleFrom = DalModule.BlackList.class, stateful = true, provided = false)
public class DalModule {

    @Ui
    class BlackList implements io.gunmetal.BlackList {
    }

    @Inject @Overrides(allowFieldInjection = true, allowCycle = true, allowPluralQualifier = true)
    @Dal @Bl DalModule dalModule;

    @Provides public DongleDao dongleDao() {
        return new DongleDao() {
        };
    }

    @Provides @Overrides(allowPluralQualifier = true, allowCycle = true) @Bl public DalModule dalModule() {
        return this;
    }

}
