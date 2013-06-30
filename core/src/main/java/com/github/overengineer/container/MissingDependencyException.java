package com.github.overengineer.container;

import com.github.overengineer.container.key.Dependency;

/**
 * @author rees.byars
 */
public class MissingDependencyException extends RuntimeException {
    public MissingDependencyException(Dependency dependency) {
        super("No components of type [" + dependency.getTypeKey().getType() + "] with qualifier [" + dependency.getQualifier() + "] have been registered with the container");
    }
}
