package io.gunmetal.sandbox.testmocks.dongle.ws;

import io.gunmetal.AccessLevel;
import io.gunmetal.FromModule;
import io.gunmetal.Module;
import io.gunmetal.Supplies;
import io.gunmetal.sandbox.testmocks.dongle.auth.AuthAdapter;
import io.gunmetal.sandbox.testmocks.dongle.auth.AuthLib;
import io.gunmetal.sandbox.testmocks.dongle.auth.AuthService;
import io.gunmetal.sandbox.testmocks.dongle.bl.DongleService;
import io.gunmetal.sandbox.testmocks.dongle.bl.IBlModule;
import io.gunmetal.sandbox.testmocks.dongle.layers.Bl;
import io.gunmetal.sandbox.testmocks.dongle.layers.Ws;

/**
 * @author rees.byars
 */
@Ws
@Module(
        subsumes = AuthLib.class,
        dependsOn = IBlModule.class,
        onlyAccessibleFrom = WsModule.WhiteList.class,
        access = AccessLevel.PRIVATE)
public class WsModule {

    @Ws
    class WhiteList {
    }

    @Supplies static DongleResource dongleResource(@Bl DongleService dongleService,
                                                   @FromModule AuthService authService) {
        return new DongleResource();
    }

    @Supplies static AuthAdapter authAdapter() {
        return new AuthAdapter() {
        };
    }

}
