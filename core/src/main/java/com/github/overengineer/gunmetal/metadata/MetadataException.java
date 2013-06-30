package com.github.overengineer.gunmetal.metadata;

/**
 * @author rees.byars
 */
public class MetadataException extends RuntimeException {
    public MetadataException(String message, Throwable root) {
        super(message, root);
    }
}
