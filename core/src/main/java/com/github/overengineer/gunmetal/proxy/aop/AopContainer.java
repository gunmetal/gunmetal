package com.github.overengineer.gunmetal.proxy.aop;

import com.github.overengineer.gunmetal.proxy.HotSwappableContainer;

import java.util.List;

/**
 * @author rees.byars
 */
public interface AopContainer extends HotSwappableContainer {
    <A extends Aspect<?>> AopContainer addAspect(Class<A> aspectClass);
    List<Aspect> getAspects();
}
