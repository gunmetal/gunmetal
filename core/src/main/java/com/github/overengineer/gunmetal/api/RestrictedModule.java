package com.github.overengineer.gunmetal.api;

/**
 * @author rees.byars
 */
public interface RestrictedModule {

    Class[] NONE = {};

    Class[] notAccessibleFrom();
    Class[] onlyAccessibleFrom();

}
