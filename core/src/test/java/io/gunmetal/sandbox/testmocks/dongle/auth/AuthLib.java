package io.gunmetal.sandbox.testmocks.dongle.auth;

import io.gunmetal.FromModule;
import io.gunmetal.Module;
import io.gunmetal.Supplies;

/**
 * @author rees.byars
 */
@Module(lib = true, type = Module.Type.CONSTRUCTED)
public class AuthLib {

    @Supplies private AuthService authService(@FromModule AuthAdapter authAdapter,
                                              @FromModule Author author,
                                              @FromModule Authorizer authorizer,
                                              @FromModule Auteur auteur) {
        return new AuthService();
    }

    @Supplies Authorizer authorizer(@FromModule AuthAdapter authAdapter) {
        return () -> false;
    }

    @Supplies Auteur authorizer(@FromModule Author author) {
        return author;
    }

}
