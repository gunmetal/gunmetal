package io.gunmetal.builder;

import io.gunmetal.ApplicationContainer;

/**
 * @author rees.byars
 */
public interface TwoPhaseApplicationBuilder {
    ApplicationContainer build(Class<?> application);
}
