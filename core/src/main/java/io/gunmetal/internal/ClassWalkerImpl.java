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

import io.gunmetal.Overrides;
import io.gunmetal.spi.ClassWalker;
import io.gunmetal.spi.InjectionResolver;
import io.gunmetal.spi.ProvisionErrors;
import io.gunmetal.spi.ResourceMetadata;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
class ClassWalkerImpl implements ClassWalker {

    private final InjectionResolver injectionResolver;
    private final boolean restrictFieldInjection;
    private final boolean restrictSetterInjection;

    ClassWalkerImpl(InjectionResolver injectionResolver,
                    boolean restrictFieldInjection,
                    boolean restrictSetterInjection) {
        this.injectionResolver = injectionResolver;
        this.restrictFieldInjection = restrictFieldInjection;
        this.restrictSetterInjection = restrictSetterInjection;
    }

    @Override public void walk(Class<?> classToWalk,
                               InjectedMemberVisitor<Field> fieldVisitor,
                               InjectedMemberVisitor<Method> methodVisitor,
                               ResourceMetadata<?> resourceMetadata,
                               ProvisionErrors errors) {
        for (Class<?> cls = classToWalk; cls != Object.class; cls = cls.getSuperclass()) {
            for (Field field : cls.getDeclaredFields()) {
                if (injectionResolver.shouldInject(field)) {
                    if (restrictFieldInjection && !resourceMetadata.overrides().allowFieldInjection()) {
                        Overrides overrides = field.getAnnotation(Overrides.class);
                        if (overrides == null || !overrides.allowFieldInjection()) {
                            errors.add("Field injection restricted [" + field + "]");
                        }
                    }
                    fieldVisitor.visit(field);
                }
            }
            for (Method method : cls.getDeclaredMethods()) {
                if (injectionResolver.shouldInject(method)) {
                    if (restrictSetterInjection && !resourceMetadata.overrides().allowSetterInjection()) {
                        Overrides overrides = method.getAnnotation(Overrides.class);
                        if (overrides == null || !overrides.allowSetterInjection()) {
                            errors.add("Method injection restricted [" + method + "]");
                        }
                    }
                    methodVisitor.visit(method);
                }
            }
        }
    }
}
