package io.gunmetal;

/**
 * @author rees.byars
 */
public interface CompositeQualifier {

    Object[] getQualifiers();

    boolean intersects(Object[] qualifiers);

    boolean intersects(CompositeQualifier compositeQualifier);

}
