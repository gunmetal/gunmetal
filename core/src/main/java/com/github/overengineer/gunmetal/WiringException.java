package com.github.overengineer.gunmetal;

/**
 * @author rees.byars
 */
public class WiringException extends Exception {

    private static final long serialVersionUID = 6546605375906484207L;

    public WiringException(String message, Exception root) {
        super(message, root);
    }
}
