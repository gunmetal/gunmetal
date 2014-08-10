package io.gunmetal.testmocks.dongle.bl;

import io.gunmetal.AutoCollection;
import io.gunmetal.FromModule;
import io.gunmetal.Lazy;
import io.gunmetal.Module;
import io.gunmetal.Provider;
import io.gunmetal.Provides;
import io.gunmetal.Ref;
import io.gunmetal.testmocks.dongle.dal.DalModule;
import io.gunmetal.testmocks.dongle.dal.DongleDao;
import io.gunmetal.testmocks.dongle.layers.Bl;
import io.gunmetal.testmocks.dongle.layers.Dal;

import java.util.List;

/**
 * @author rees.byars
 */
@Bl @Module(dependsOn = DalModule.class, notAccessibleFrom = BlModule.BlackList.class)
public class BlModule {

    @Dal class BlackList implements io.gunmetal.BlackList { }

    @Provides public static DongleService dongleService(@Dal DongleDao dongleDao,
                                              @FromModule DonglerFactory donglerFactory,
                                              @FromModule Ref<RuntimeException> runtimeExceptionRef) {
        return new DongleService() { };
    }

    @Provides @AutoCollection static Dongler dongler1() {
        return new Dongler();
    }

    @Provides @AutoCollection static Dongler dongler2() {
        return new Dongler();
    }

    @Provides static DonglerFactory donglerFactory(@AutoCollection @Bl Provider<List<Dongler>> donglers) {
        return name -> new Dongler(name, donglers.get());
    }

    @Provides @Lazy static RuntimeException runtimeException() {
        throw new UnsupportedOperationException("this dependency should not be instantiated");
    }

}
