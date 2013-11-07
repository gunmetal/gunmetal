package io.gunmetal.builder;

import com.github.overengineer.gunmetal.key.Dependency;

import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
interface DependencyRequest {

    Class<?> getSourceComponentClass();

    CompositeQualifier getSourceQualifier();

    ModuleAdapter getSourceModule();

    Dependency<?> getDependency();

    List<String> getErrors();

    DependencyRequest addError(String errorMessage);

    final class Factory {

        private Factory() { }

        static DependencyRequest create(final ComponentMetadata<?> componentMetadata, final Dependency<?> dependency) {

            return new DependencyRequest() {

                List<String> errors;

                @Override
                public Class<?> getSourceComponentClass() {
                    return componentMetadata.getComponentClass();
                }

                @Override
                public CompositeQualifier getSourceQualifier() {
                    return componentMetadata.getCompositeQualifier();
                }

                @Override
                public ModuleAdapter getSourceModule() {
                    return componentMetadata.getModuleAdapter();
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
