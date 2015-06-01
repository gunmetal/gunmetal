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

package io.gunmetal;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author rees.byars
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Module {

    Class<?>[] dependsOn() default {};

    Class<?>[] subsumes() default {};

    Class<?>[] notAccessibleFrom() default {};

    Class<?>[] onlyAccessibleFrom() default {};

    AccessLevel access() default AccessLevel.UNDEFINED;

    boolean lib() default false;

    boolean component() default false;

    Module NONE = new Module() {

        @Override public Class<? extends Annotation> annotationType() {
            return Module.class;
        }

        @Override public Class<?>[] dependsOn() {
            return new Class<?>[0];
        }

        @Override public Class<?>[] subsumes() {
            return new Class<?>[0];
        }

        @Override public Class<?>[] notAccessibleFrom() {
            return new Class<?>[0];
        }

        @Override public Class<?>[] onlyAccessibleFrom() {
            return new Class<?>[0];
        }

        @Override public AccessLevel access() {
            return AccessLevel.UNDEFINED;
        }

        @Override public boolean lib() {
            return false;
        }

        @Override public boolean component() {
            return false;
        }

    };

}
