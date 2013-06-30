package com.github.overengineer.gunmetal.proxy;

import com.github.overengineer.gunmetal.Container;

/**
 * @author rees.byars
 */
public interface HotSwappableContainer extends Container {

    <T> void swap(Class<T> target, Class<? extends T> implementation) throws HotSwapException;

    <T, I extends T> void swap(Class<T> target, I implementation) throws HotSwapException;

}
