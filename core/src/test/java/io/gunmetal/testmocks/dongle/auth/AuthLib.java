package io.gunmetal.testmocks.dongle.auth;

import io.gunmetal.FromModule;
import io.gunmetal.Module;
import io.gunmetal.Provides;

/**
 * @author rees.byars
 */
@Module(lib = true, provided = false, stateful = true)
public class AuthLib {

    @Provides private AuthService authService(@FromModule AuthAdapter authAdapter,
                                              @FromModule Author author,
                                              @FromModule Authorizer authorizer) {
        return new AuthService();
    }

    @Provides Authorizer authorizer(@FromModule AuthAdapter authAdapter) {
        return () -> false;
    }

}
