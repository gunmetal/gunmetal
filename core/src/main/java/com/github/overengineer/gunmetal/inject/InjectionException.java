package com.github.overengineer.gunmetal.inject;

/**
 * @author rees.byars
 */
public class InjectionException extends RuntimeException {
    public InjectionException(String message, Exception root) {
        super(message, root);
    }
}
