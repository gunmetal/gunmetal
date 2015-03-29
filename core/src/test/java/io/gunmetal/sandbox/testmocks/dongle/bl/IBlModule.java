package io.gunmetal.sandbox.testmocks.dongle.bl;

import io.gunmetal.FromModule;
import io.gunmetal.Lazy;
import io.gunmetal.Module;
import io.gunmetal.MultiBind;
import io.gunmetal.Provides;
import io.gunmetal.Ref;
import io.gunmetal.Singleton;
import io.gunmetal.sandbox.testmocks.dongle.dal.DalModule;
import io.gunmetal.sandbox.testmocks.dongle.dal.DongleDao;
import io.gunmetal.sandbox.testmocks.dongle.layers.Bl;
import io.gunmetal.sandbox.testmocks.dongle.layers.Dal;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author rees.byars
 */
@Bl
@Module(dependsOn = DalModule.class,
        notAccessibleFrom = IBlModule.BlackList.class,
        type = Module.Type.PROVIDED)
public interface IBlModule {

    @Dal
    class BlackList implements io.gunmetal.BlackList {
    }

    @Provides public DongleService dongleService(@Dal DongleDao dongleDao,
                                                 @FromModule DonglerFactory donglerFactory,
                                                 @FromModule Ref<RuntimeException> runtimeExceptionRef);

    @Provides @MultiBind static Dongler dongler1() {
        return new Dongler();
    }

    @Provides @MultiBind static Dongler dongler2() {
        return new Dongler();
    }

    @Provides static DonglerFactory donglerFactory(
            @MultiBind @Bl Supplier<List<Dongler>> donglers) {
        List<Dongler> donglers1 = donglers.get();
        return name -> new Dongler(name, donglers1);
    }

    @Provides @Lazy static RuntimeException runtimeException() {
        throw new UnsupportedOperationException("this dependency should not be instantiated");
    }

    @Provides @Singleton static IBlModule me() {
        return new BlModule();
    }

}
