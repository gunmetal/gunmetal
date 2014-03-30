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

import io.gunmetal.spi.ComponentMetadata;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface InjectorFactory {
    StaticInjector staticInjector(Method method, ComponentMetadata<?> componentMetadata);
    <T> Injector<T> compositeInjector(ComponentMetadata<Class<?>> componentMetadata);
    <T> Injector<T> lazyCompositeInjector(ComponentMetadata<?> componentMetadata);
    <T> Instantiator<T> constructorInstantiator(ComponentMetadata<Class<?>> componentMetadata);
    <T> Instantiator<T> methodInstantiator(ComponentMetadata<Method> componentMetadata);
}
