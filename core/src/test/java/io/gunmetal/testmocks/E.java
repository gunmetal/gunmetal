package io.gunmetal.testmocks;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author rees.byars
 */
@Singleton
public class E {
    @Inject
    public E(F f) { }
}
