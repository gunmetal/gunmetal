package io.gunmetal.internal;

import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;

/**
 * @author rees.byars
 */
interface BindingProxy<T> extends Replicable<Binding<T>> {

    DependencyResponse<T> service(DependencyRequest<? super T> dependencyRequest, Errors errors);

    Binding<T> binding();

}
