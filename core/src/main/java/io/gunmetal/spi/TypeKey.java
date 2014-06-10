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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author rees.byars
 */
public final class TypeKey<T> {

    private final Type type;
    private final Class<? super T> raw;
    private final int hash;

    TypeKey(Type type, Class<? super T> raw) {
        this.type = type;
        this.raw = raw;
        // TODO this could be better
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            hash = Objects.hash(
                    Arrays.hashCode(parameterizedType.getActualTypeArguments()),
                    parameterizedType.getRawType(),
                    parameterizedType.getOwnerType());
        } else {
            hash = type.hashCode();
        }
    }

    public Type type() {
        return type;
    }

    public Class<? super T> raw() {
        return raw;
    }

    @Override public int hashCode() {
        return hash;
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

    @Override public String toString() {
        return "typeKey[ raw:" + raw.getName() + ", type:" + type.getTypeName() + " ]";
    }

}
