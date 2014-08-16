package io.gunmetal.spi;

/**
 * @author rees.byars
 */
public interface Errors {

    Errors add(ProvisionMetadata<?> provisionMetadata, String errorMessage);

    Errors add(String errorMessage);

}
