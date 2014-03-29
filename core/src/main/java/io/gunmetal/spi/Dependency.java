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

import io.gunmetal.internal.Smithy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
public abstract class Dependency<T> {

    public abstract Qualifier qualifier();

    public abstract TypeKey<T> typeKey();

    @Override public int hashCode() {
        return typeKey().hashCode() * 67 + qualifier().hashCode();
    }

    @Override public boolean equals(Object target) {
        if (target == this) {
            return true;
        }
        if (!(target instanceof Dependency<?>)) {
            return false;
        }
        Dependency<?> dependencyTarget = (Dependency<?>) target;
        return dependencyTarget.qualifier().equals(qualifier())
                && dependencyTarget.typeKey().equals(typeKey());
    }

    @Override public String toString() {
        return "dependency[" + qualifier().toString() + typeKey().toString() + "]";
    }

    public static <T> Dependency<T> from(final Qualifier qualifier, Class<T> cls) {
        final TypeKey<T> typeKey = Types.typeKey(cls);
        return new Dependency<T>() {
            @Override public Qualifier qualifier() {
                return qualifier;
            }
            @Override public TypeKey<T> typeKey() {
                return typeKey;
            }
        };
    }

    public static <T> Dependency<T> from(final Qualifier qualifier, Type type) {
        final TypeKey<T> typeKey = Types.typeKey(type);
        return new Dependency<T>() {
            @Override public Qualifier qualifier() {
                return qualifier;
            }
            @Override public TypeKey<T> typeKey() {
                return typeKey;
            }
        };
    }

    public static <T> Dependency<T> from(final Qualifier qualifier, ParameterizedType type) {
        final TypeKey<T> typeKey = Types.typeKey(type);
        return new Dependency<T>() {
            @Override public Qualifier qualifier() {
                return qualifier;
            }
            @Override public TypeKey<T> typeKey() {
                return typeKey;
            }
        };
    }

    public static List<Dependency<?>> from(final Qualifier qualifier, Class<?>[] classes) {
        List<Dependency<?>> dependencies = new LinkedList<>();
        for (Class<?> cls : classes) {
            dependencies.add(from(qualifier, cls));
        }
        return dependencies;
    }


    private static final class Types {

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

        static <T> TypeKey<T> typeKey(final Type type) {
            if (type instanceof Class) {
                return Smithy.cloak(typeKey((Class) type));
            } else if (type instanceof ParameterizedType) {
                return typeKey(((ParameterizedType) type));
            } else {
                throw new UnsupportedOperationException("The type [" + type + "] is currently unsupported");
            }
        }

        static <T> TypeKey<T> typeKey(final ParameterizedType type) {
            final Class<? super T> raw = Smithy.cloak(type.getRawType());
            return new TypeKey<T>() {
                @Override public Type type() {
                    return type;
                }
                @Override public Class<? super T> raw() {
                    return raw;
                }
            };
        }

    }

}
