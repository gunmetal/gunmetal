package io.gunmetal.testmocks.dongle.auth;

import io.gunmetal.FromModule;
import io.gunmetal.Module;

/**
 * @author rees.byars
 */
@Module(lib = true, provided = false, stateful = true)
public class AuthLib {

    private AuthService authService(@FromModule AuthAdapter authAdapter) {
        return new AuthService();
    }

}
