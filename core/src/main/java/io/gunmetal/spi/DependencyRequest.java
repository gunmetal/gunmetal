/*
 * Copyright (c) 2013.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gunmetal.spi;

import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
public interface DependencyRequest {

    Class<?> sourceOrigin();

    Qualifier sourceQualifier();

    ModuleMetadata sourceModule();

    Dependency<?> dependency();

    List<String> errors();

    DependencyRequest addError(String errorMessage);

    final class Factory {

        private Factory() { }

        static DependencyRequest create(final ComponentMetadata<?> requestingComponent, final Dependency<?> dependency) {

            return new DependencyRequest() {

                List<String> errors;

                @Override
                public Class<?> sourceOrigin() {
                    return requestingComponent.providerClass();
                }

                @Override
                public Qualifier sourceQualifier() {
                    return requestingComponent.qualifier();
                }

                @Override
                public ModuleMetadata sourceModule() {
                    return requestingComponent.moduleMetadata();
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
