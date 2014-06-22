package io.gunmetal.testmocks.dongle.ws;

import io.gunmetal.AccessLevel;
import io.gunmetal.FromModule;
import io.gunmetal.Module;
import io.gunmetal.testmocks.dongle.auth.AuthAdapter;
import io.gunmetal.testmocks.dongle.auth.AuthLib;
import io.gunmetal.testmocks.dongle.auth.AuthService;
import io.gunmetal.testmocks.dongle.bl.BlModule;
import io.gunmetal.testmocks.dongle.bl.DongleService;
import io.gunmetal.testmocks.dongle.layers.Bl;
import io.gunmetal.testmocks.dongle.layers.Ws;

/**
 * @author rees.byars
 */
@Ws
@Module(
    subsumes = AuthLib.class,
    dependsOn = BlModule.class,
    onlyAccessibleFrom = WsModule.WhiteList.class,
    access = AccessLevel.PRIVATE)
public class WsModule {

    @Ws class WhiteList implements io.gunmetal.WhiteList { }

    static DongleResource dongleResource(@Bl DongleService dongleService,
                                         @FromModule AuthService authService) {
        return new DongleResource();
    }

    static AuthAdapter authAdapter() {
        return new AuthAdapter() { };
    }

}
