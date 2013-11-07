package io.gunmetal.internal;

import io.gunmetal.ApplicationContainer;

/**
 * @author rees.byars
 */
public interface ApplicationBuilder {

    ApplicationContainer build(Class<?> application);

}
