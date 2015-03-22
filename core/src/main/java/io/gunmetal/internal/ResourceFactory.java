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
import io.gunmetal.spi.ResourceMetadata;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;

/**
 * @author rees.byars
 */
interface ResourceFactory {

    Resource withClassProvider(ResourceMetadata<Class<?>> resourceMetadata,
                               GraphContext context);

    <M extends AnnotatedElement & Member> Resource withMemberProvider(
            ResourceMetadata<M> resourceMetadata,
            Dependency moduleDependency,
            GraphContext context);

    Resource withProvidedModule(ResourceMetadata<Class<?>> resourceMetadata, GraphContext context);

}
