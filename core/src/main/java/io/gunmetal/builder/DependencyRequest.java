package io.gunmetal.builder;

import com.github.overengineer.gunmetal.key.Dependency;

import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
interface DependencyRequest {

    ComponentMetadata<?> getRequestSource();

    Dependency<?> getDependency();

    List<String> getErrors();

    DependencyRequest addError(String errorMessage);

    class Factory {

        static DependencyRequest create(final ComponentAdapter<?> componentAdapter, final Dependency<?> dependency) {

            return new DependencyRequest() {

                List<String> errors;

                @Override
                public ComponentAdapter<?> getRequestSource() {
                    return componentAdapter;
                }

                @Override
                public Dependency<?> getDependency() {
                    return dependency;
                }

                @Override
                public List<String> getErrors() {
                    return errors;
                }

                @Override
                public DependencyRequest addError(String errorMessage) {
                    if (errors == null) {
                        errors = new LinkedList<String>();
                    }
                    errors.add(errorMessage);
                    return this;
                }

            };

        }

    }

}
