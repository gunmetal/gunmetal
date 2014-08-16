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

import io.gunmetal.spi.ProvisionMetadata;
import io.gunmetal.spi.Dependency;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface InjectorFactory {
    <T> Injector<T> compositeInjector(ProvisionMetadata<Class<?>> provisionMetadata, GraphContext context);
    <T> Injector<T> lazyCompositeInjector(ProvisionMetadata<?> provisionMetadata, GraphContext context);
    <T> Instantiator<T> constructorInstantiator(ProvisionMetadata<Class<?>> provisionMetadata, GraphContext context);
    <T> Instantiator<T> methodInstantiator(ProvisionMetadata<Method> provisionMetadata, GraphContext context);
    <T> Instantiator<T> instanceInstantiator(ProvisionMetadata<Class<?>> provisionMetadata, GraphContext context);
    <T, S> Instantiator<T> statefulMethodInstantiator(
            ProvisionMetadata<Method> provisionMetadata, Dependency<S> moduleDependency, GraphContext context);
}
