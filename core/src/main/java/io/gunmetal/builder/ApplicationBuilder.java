package io.gunmetal.builder;

import io.gunmetal.ApplicationContainer;

/**
 * @author rees.byars
 */
public interface ApplicationBuilder {

    ApplicationContainer build(Class<?> application);

}
