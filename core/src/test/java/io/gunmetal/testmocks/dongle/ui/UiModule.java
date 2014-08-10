package io.gunmetal.testmocks.dongle.ui;

import io.gunmetal.FromModule;
import io.gunmetal.Module;
import io.gunmetal.Provides;
import io.gunmetal.testmocks.dongle.auth.AuthAdapter;
import io.gunmetal.testmocks.dongle.auth.AuthLib;
import io.gunmetal.testmocks.dongle.auth.AuthService;
import io.gunmetal.testmocks.dongle.bl.BlModule;
import io.gunmetal.testmocks.dongle.bl.DongleService;
import io.gunmetal.testmocks.dongle.layers.Bl;
import io.gunmetal.testmocks.dongle.layers.Ui;
import io.gunmetal.testmocks.dongle.scope.Thread;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
@Ui @Module(
        subsumes = AuthLib.class,
        dependsOn = {BlModule.class, UserModule.class},
        onlyAccessibleFrom = UiModule.WhiteList.class)
public class UiModule {

    @Ui class WhiteList implements io.gunmetal.WhiteList { }

    @Provides @Thread static DongleController dongleController(
            @Bl DongleService dongleService,
            @FromModule AuthService authService,
            @FromModule Map<String, String> requestContext) {
        return new DongleController();
    }

    @Provides @Thread private static Map<String, String> requestContext(@FromModule User user) {
        return new HashMap<>();
    }

    @Provides private static AuthAdapter authAdapter() {
        return new AuthAdapter() { };
    }

}
