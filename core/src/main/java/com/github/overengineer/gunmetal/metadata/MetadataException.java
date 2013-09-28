package com.github.overengineer.gunmetal.metadata;

/**
 * @author rees.byars
 */
public class MetadataException extends RuntimeException {

    private static final long serialVersionUID = -3857913153887479239L;

    public MetadataException(String message, Throwable root) {
        super(message, root);
    }
}
