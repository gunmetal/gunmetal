package io.gunmetal.internal;

import io.gunmetal.CompositeQualifier;
import io.gunmetal.Dependency;

import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
interface DependencyRequest {

    Class<?> sourceOrigin();

    CompositeQualifier sourceQualifier();

    ModuleAdapter sourceModule();

    Dependency<?> dependency();

    List<String> errors();

    DependencyRequest addError(String errorMessage);

    final class Factory {

        private Factory() { }

        static DependencyRequest create(final ComponentMetadata componentMetadata, final Dependency<?> dependency) {

            return new DependencyRequest() {

                List<String> errors;

                @Override
                public Class<?> sourceOrigin() {
                    return componentMetadata.providerClass();
                }

                @Override
                public CompositeQualifier sourceQualifier() {
                    return componentMetadata.compositeQualifier();
                }

                @Override
                public ModuleAdapter sourceModule() {
                    return componentMetadata.moduleAdapter();
                }

                @Override
                public Dependency<?> dependency() {
                    return dependency;
                }

                @Override
                public List<String> errors() {
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
