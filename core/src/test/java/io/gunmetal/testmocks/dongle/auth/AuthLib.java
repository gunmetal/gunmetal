package io.gunmetal.testmocks.dongle.auth;

import io.gunmetal.FromModule;
import io.gunmetal.Library;

/**
 * @author rees.byars
 */
@Library
public class AuthLib {

    private static AuthService authService(@FromModule AuthAdapter authAdapter) {
        return new AuthService();
    }

}
