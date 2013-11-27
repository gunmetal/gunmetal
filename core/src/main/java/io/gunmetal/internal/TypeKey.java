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

import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
abstract class TypeKey<T> {

    abstract Type type();

    abstract Class<? super T> raw();

    @Override public int hashCode() {
        return type().hashCode();
    }

    @Override public boolean equals(Object target) {
        if (target == this) {
            return true;
        }
        if (!(target instanceof TypeKey<?>)) {
            return false;
        }
        TypeKey<?> typeKeyTarget = (TypeKey<?>) target;
        return type().equals(typeKeyTarget.type());
    }

}
