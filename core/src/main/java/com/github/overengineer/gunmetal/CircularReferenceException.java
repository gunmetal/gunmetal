package com.github.overengineer.gunmetal;

/**
 * @author rees.byars
 */
public class CircularReferenceException  extends RuntimeException {
    protected CircularReferenceException(String message) {
        super(message);
    }
}
