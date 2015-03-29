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

    Class<? extends BlackList> notAccessibleFrom() default BlackList.class;

    Class<? extends WhiteList> onlyAccessibleFrom() default WhiteList.class;

    AccessLevel access() default AccessLevel.UNDEFINED;

    Type type() default Type.STATELESS;

    boolean lib() default false;

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

        @Override public Class<? extends BlackList> notAccessibleFrom() {
            return BlackList.class;
        }

        @Override public Class<? extends WhiteList> onlyAccessibleFrom() {
            return WhiteList.class;
        }

        @Override public AccessLevel access() {
            return AccessLevel.UNDEFINED;
        }

        @Override public Type type() {
            return Type.STATELESS;
        }

        @Override public boolean lib() {
            return false;
        }

    };

    public enum Type {
        STATELESS, COMPONENT_PARAM, CONSTRUCTED, PROVIDED
    }

}
