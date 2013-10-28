package com.github.overengineer.gunmetal.builder;

import com.github.overengineer.gunmetal.api.ApplicationContainer;

/**
 * @author rees.byars
 */
public interface TwoPhaseApplicationBuilder {
    ApplicationContainer build(Class<?> application);
}
