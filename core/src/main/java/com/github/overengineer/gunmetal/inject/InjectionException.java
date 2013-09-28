package com.github.overengineer.gunmetal.inject;

/**
 * @author rees.byars
 */
public class InjectionException extends RuntimeException {

    private static final long serialVersionUID = -593836828812845808L;

    public InjectionException(String message, Exception root) {
        super(message, root);
    }
}
