package com.github.overengineer.gunmetal;

import com.github.overengineer.gunmetal.key.Dependency;

/**
 * @author rees.byars
 */
public class MissingDependencyException extends RuntimeException {

    private static final long serialVersionUID = -2835646489248218279L;

    public MissingDependencyException(Dependency dependency) {
        super("No components of type [" + dependency.getTypeKey().getType() + "] with qualifier [" + dependency.getQualifier() + "] have been registered with the container");
    }
}
