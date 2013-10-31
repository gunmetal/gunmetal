package io.gunmetal.builder;

import io.gunmetal.ApplicationContainer;

/**
 * @author rees.byars
 */
public interface ApplicationBuilder {

    //instead of the two phase approach, go with a proxy approach?  inject "empty" proxy that is filled later
    //but for eager singleton?? - check the scope ;)

    ApplicationContainer build(Class<?> application);
}
