package io.gunmetal.testmocks;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author rees.byars
 */
@Singleton
@io.gunmetal.Singleton
public class E {
    @Inject
    public E(F f) { }
}
