package io.gunmetal.testmocks.dongle.auth;

/**
 * @author rees.byars
 */
public class Author implements Authorizer, Auteur {

    @Override public boolean isAuthorized() {
        return true;
    }

    // @Inject @FromModule @Overrides(allowFieldInjection = true) AuthService authService;

}
