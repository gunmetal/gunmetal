package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface Qualifier {

    Object[] qualifiers();

    boolean intersects(Object[] qualifiers);

    boolean intersects(Qualifier qualifier);

}
