package com.github.overengineer.gunmetal.proxy.aop;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface Aspect<T> extends Serializable {

    Object advise(JoinPoint<T> invocation) throws Throwable;

}
