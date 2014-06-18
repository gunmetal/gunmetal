package io.gunmetal.testmocks.dongle.ui;

import io.gunmetal.FromModule;
import io.gunmetal.Module;
import io.gunmetal.testmocks.dongle.auth.AuthAdapter;
import io.gunmetal.testmocks.dongle.auth.AuthModule;
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
@Ui @Module(subsumes = AuthModule.class, dependsOn = BlModule.class, onlyAccessibleFrom = UiModule.WhiteList.class)
public class UiModule {

    @Ui class WhiteList implements io.gunmetal.WhiteList { }

    @Thread static DongleController dongleController(
            @Bl DongleService dongleService,
            @FromModule AuthService authService,
            @FromModule Map<String, String> requestContext) {
        return new DongleController();
    }

    @Thread private static Map<String, String> requestContext() {
        return new HashMap<>();
    }

    private static AuthAdapter authAdapter() {
        return new AuthAdapter() { };
    }

}
