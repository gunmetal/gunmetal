package io.gunmetal.builder;

import com.github.overengineer.gunmetal.key.Dependency;

/**
 * @author rees.byars
 */
interface DependencyRequest {

    ComponentAdapter<?> getRequestSource();

    Dependency<?> getDependency();

}
