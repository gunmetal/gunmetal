package io.gunmetal.sandbox.testmocks.dongle.ui;

import io.gunmetal.FromModule;
import io.gunmetal.Module;
import io.gunmetal.Supplies;
import io.gunmetal.sandbox.testmocks.dongle.auth.AuthAdapter;
import io.gunmetal.sandbox.testmocks.dongle.auth.AuthLib;
import io.gunmetal.sandbox.testmocks.dongle.auth.AuthService;
import io.gunmetal.sandbox.testmocks.dongle.bl.DongleService;
import io.gunmetal.sandbox.testmocks.dongle.bl.IBlModule;
import io.gunmetal.sandbox.testmocks.dongle.layers.Bl;
import io.gunmetal.sandbox.testmocks.dongle.layers.Ui;
import io.gunmetal.sandbox.testmocks.dongle.scope.Thread;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
@Ui
@Module(
        subsumes = AuthLib.class,
        dependsOn = {IBlModule.class, UserModule.class},
        onlyAccessibleFrom = UiModule.WhiteList.class)
public class UiModule {

    @Ui
    class WhiteList implements io.gunmetal.WhiteList {
    }

    @Supplies @Thread static DongleController dongleController(
            @Bl DongleService dongleService,
            @FromModule AuthService authService,
            @FromModule Map<String, String> requestContext) {
        return new DongleController();
    }

    @Supplies @Thread private static Map<String, String> requestContext(@FromModule User user) {
        return new HashMap<>();
    }

    @Supplies private static AuthAdapter authAdapter(@FromModule FactoryTest factoryTest) {
        return new AuthAdapter() {
        };
    }

}
