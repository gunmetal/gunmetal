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

package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.ResolutionContext;

import java.util.Collections;
import java.util.List;

/**
 * @author rees.byars
 */
interface Injector extends Dependent, Replicable<Injector> {

    Object inject(Object target, DependencySupplier dependencySupplier, ResolutionContext resolutionContext);

    Injector NONE = new Injector() {
        @Override public Object inject(Object target,
                                       DependencySupplier dependencySupplier,
                                       ResolutionContext resolutionContext) {
            return null;
        }

        @Override public List<Dependency> dependencies() {
            return Collections.emptyList();
        }

        @Override public Injector replicateWith(ComponentContext context) {
            return this;
        }
    };

}
