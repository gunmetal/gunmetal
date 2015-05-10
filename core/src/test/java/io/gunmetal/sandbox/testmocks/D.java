package io.gunmetal.sandbox.testmocks;

import javax.inject.Inject;

/**
 * @author rees.byars
 */
public class D {

    public E e;

    @Inject
    public D(E e) {
        this.e = e;
    }

}
