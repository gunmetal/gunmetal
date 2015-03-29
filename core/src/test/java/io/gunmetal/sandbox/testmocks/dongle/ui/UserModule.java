package io.gunmetal.sandbox.testmocks.dongle.ui;

import io.gunmetal.Module;
import io.gunmetal.Provides;
import io.gunmetal.Singleton;
import io.gunmetal.sandbox.testmocks.dongle.layers.Ui;

/**
 * @author rees.byars
 */
@Ui
@Singleton
@Module(type = Module.Type.COMPONENT_PARAM)
public class UserModule {

    private final User user;

    public UserModule(String name) {
        user = new User(name);
    }

    @Provides public User user() {
        return user;
    }

}
