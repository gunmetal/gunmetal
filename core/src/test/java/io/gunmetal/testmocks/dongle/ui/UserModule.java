package io.gunmetal.testmocks.dongle.ui;

import io.gunmetal.Module;
import io.gunmetal.Provides;
import io.gunmetal.Singleton;
import io.gunmetal.testmocks.dongle.layers.Ui;

/**
 * @author rees.byars
 */
@Ui @Singleton @Module(stateful = true)
public class UserModule {
    
    private final User user;

    public UserModule(String name) {
        user = new User(name);
    }

    @Provides public User user() {
        return user;
    }
    
}
