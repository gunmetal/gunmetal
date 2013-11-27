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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author rees.byars
 */
final class Types {

    private Types() { }

    static <T> TypeKey<T> typeKey(final Class<T> cls) {
        return new TypeKey<T>() {
            @Override public Type type() {
                return cls;
            }
            @Override public Class<? super T> raw() {
                return cls;
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <T> TypeKey<T> typeKey(final Type type) {
        if (type instanceof Class) {
            return typeKey((Class) type);
        } else if (type instanceof ParameterizedType) {
            return typeKey(((ParameterizedType) type));
        } else {
            throw new UnsupportedOperationException("The type [" + type + "] is currently unsupported");
        }
    }

    @SuppressWarnings("unchecked")
    static <T> TypeKey<T> typeKey(final ParameterizedType type) {
        final Class<? super T> raw = (Class<? super T>) type.getRawType();
        return new TypeKey<T>() {
            @Override public Type type() {
                return type;
            }
            @Override public Class<? super T> raw() {
                return raw;
            }
        };
    }

    static Collection<TypeKey<?>> typeKeys(final Class<?>[] classes) {
        List<TypeKey<?>> typeKeys = new ArrayList<TypeKey<?>>();
        for (Class<?> cls : classes) {
            typeKeys.add(typeKey(cls));
        }
        return typeKeys;
    }

}
