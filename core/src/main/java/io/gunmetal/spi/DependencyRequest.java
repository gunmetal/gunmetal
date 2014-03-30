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

/**
 * @author rees.byars
 */
public interface DependencyRequest<T> {

    Class<?> sourceOrigin();

    Qualifier sourceQualifier();

    ModuleMetadata sourceModule();

    Dependency<T> dependency();

    final class Factory {

        private Factory() { }

        public static <T> DependencyRequest<T> create(final ComponentMetadata<?> requestingComponent, final Dependency<T> dependency) {

            return new DependencyRequest<T>() {

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
                public Dependency<T> dependency() {
                    return dependency;
                }

            };

        }

        public static <T> DependencyRequest<T> create(final DependencyRequest<?> dependencyRequest,
                                                      final Dependency<T> dependency) {

            return new DependencyRequest<T>() {

                @Override
                public Class<?> sourceOrigin() {
                    return dependencyRequest.sourceOrigin();
                }

                @Override
                public Qualifier sourceQualifier() {
                    return dependencyRequest.sourceQualifier();
                }

                @Override
                public ModuleMetadata sourceModule() {
                    return dependencyRequest.sourceModule();
                }

                @Override
                public Dependency<T> dependency() {
                    return dependency;
                }

            };

        }

    }

}
