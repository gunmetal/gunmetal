package com.github.overengineer.gunmetal.module;

/**
 * @author rees.byars
 */
public interface InstanceMapping<T> extends Mapping<T> {
    T getInstance();
}
