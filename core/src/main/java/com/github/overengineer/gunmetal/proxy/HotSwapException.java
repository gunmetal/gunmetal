package com.github.overengineer.gunmetal.proxy;

/**
 * @author rees.byars
 */
public class HotSwapException extends Exception {

    private static final long serialVersionUID = -4813845680738209873L;

    public HotSwapException(Class<?> target, Class<?> currentImpl, Class<?> newImpl) {
        super("Could not swap the [" + target.getName() + "] implementation from [" + currentImpl.getName() + "] to [" + newImpl.getName() + "]");
    }
}
