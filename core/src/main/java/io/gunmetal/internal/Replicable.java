package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface Replicable<T> {

    T replicate(GraphContext context);

}
