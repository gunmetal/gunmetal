package io.gunmetal.internal;

import io.gunmetal.spi.Linkers;

/**
 * @author rees.byars
 */
interface Replicable<T> {

    T replicate(Linkers linkers);

}
