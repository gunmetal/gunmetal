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
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Overrides {

    boolean allowMappingOverride() default false;

    boolean allowNoQualifier() default false;

    boolean allowNonInterface() default false;

    boolean allowCycle() default false;

    boolean allowSetterInjection() default false;

    boolean allowFieldInjection() default false;

    boolean allowPluralQualifier() default false;

    Overrides NONE = new Overrides() {

        @Override public Class<? extends Annotation> annotationType() {
            return Overrides.class;
        }

        @Override public boolean allowMappingOverride() {
            return false;
        }

        @Override public boolean allowNoQualifier() {
            return false;
        }

        @Override public boolean allowNonInterface() {
            return false;
        }

        @Override public boolean allowCycle() {
            return false;
        }

        @Override public boolean allowSetterInjection() {
            return false;
        }

        @Override public boolean allowFieldInjection() {
            return false;
        }

        @Override public boolean allowPluralQualifier() {
            return false;
        }

    };

}