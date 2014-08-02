package io.gunmetal.spi;

/**
 * @author rees.byars
 */
public interface Errors {

    Errors add(ComponentMetadata<?> componentMetadata, String errorMessage);

    Errors add(String errorMessage);

}
