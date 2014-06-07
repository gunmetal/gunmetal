package io.gunmetal.internal;

/**
 * @author rees.byars
 */
class DependencyException extends RuntimeException {

    private static final long serialVersionUID = -4523628359095103302L;

    DependencyException(String message) {
        super(message);
    }

}
