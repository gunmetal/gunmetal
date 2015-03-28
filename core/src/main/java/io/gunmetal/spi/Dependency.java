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

import io.gunmetal.util.Generics;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author rees.byars
 */
public final class Dependency {

    private final Qualifier qualifier;
    private final TypeKey typeKey;
    private final int hashCode;

    private Dependency(Qualifier qualifier, TypeKey typeKey) {
        this.qualifier = qualifier;
        this.typeKey = typeKey;
        hashCode = typeKey().hashCode() * 67 + qualifier().hashCode();
    }

    public Qualifier qualifier() {
        return qualifier;
    }

    public TypeKey typeKey() {
        return typeKey;
    }

    @Override public int hashCode() {
        return hashCode;
    }

    @Override public boolean equals(Object target) {
        if (target == this) {
            return true;
        }
        if (!(target instanceof Dependency)) {
            return false;
        }
        Dependency dependencyTarget = (Dependency) target;
        return dependencyTarget.qualifier().equals(qualifier())
                && dependencyTarget.typeKey().equals(typeKey());
    }

    @Override public String toString() {
        return "dependency[ " + qualifier().toString() + ", " + typeKey().toString() + " ]";
    }

    public static Dependency from(Qualifier qualifier, Type type) {
        return new Dependency(qualifier, Types.typeKey(type));
    }

    private static final class Types {

        private Types() {
        }

        static TypeKey typeKey(final Class<?> cls) {
            return new TypeKey(cls, cls);
        }

        static TypeKey typeKey(final Type type) {
            if (type instanceof Class) {
                return typeKey((Class<?>) type);
            } else if (type instanceof ParameterizedType) {
                return typeKey(((ParameterizedType) type));
            } else {
                throw new UnsupportedOperationException("The type [" + type + "] is currently unsupported");
            }
        }

        static TypeKey typeKey(final ParameterizedType type) {
            final Class<?> raw = Generics.as(type.getRawType());
            return new TypeKey(type, raw);
        }

    }

}
