package io.gunmetal.sandbox.testmocks.dongle.auth;

import io.gunmetal.FromModule;
import io.gunmetal.Inject;
import io.gunmetal.Overrides;

/**
 * @author rees.byars
 */
@FromModule
public class AuthService {

    @Inject @FromModule @Overrides(allowFieldInjection = true) AuthAdapter authAdapter;

}
