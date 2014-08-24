package io.gunmetal.spi;

/**
 * @author rees.byars
 */
public interface Errors {

    Errors add(ResourceMetadata<?> resourceMetadata, String errorMessage);

    Errors add(String errorMessage);

}
