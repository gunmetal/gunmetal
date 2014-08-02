package io.gunmetal.internal;

import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.Errors;

/**
 * @author rees.byars
 */
class FailFastErrors implements Errors {

    @Override public Errors add(ComponentMetadata<?> componentMetadata, String errorMessage) {
        throw new RuntimeException(errorMessage);
    }

    @Override public Errors add(String errorMessage) {
        throw new RuntimeException(errorMessage);
    }

    @Override public void throwIfNotEmpty() { }

}
