package io.gunmetal;

/**
 * @author rees.byars
 */
public interface RestrictedModule {

    Class[] NONE = {};

    Class[] notAccessibleFrom();
    Class[] onlyAccessibleFrom();

}
