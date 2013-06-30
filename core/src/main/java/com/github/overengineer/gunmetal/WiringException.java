package com.github.overengineer.gunmetal;

/**
 * @author rees.byars
 */
public class WiringException extends Exception {
    public WiringException(String message, Exception root) {
        super(message, root);
    }
}
