package io.gunmetal.internal;

import io.gunmetal.spi.Linkers;

/**
 * @author rees.byars
 */
interface LinkableComponentFactory<T> {

    T newInstance(Linkers linkers);

}
