package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface Replicable<T> {

    T replicateWith(ComponentContext context);

}
