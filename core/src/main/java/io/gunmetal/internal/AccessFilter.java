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

import io.gunmetal.AccessLevel;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface AccessFilter<T> {

    AnnotatedElement filteredElement();

    boolean isAccessibleTo(T target);

    interface ClassAccessFilter extends AccessFilter<Class<?>> {
        boolean isPublic();
    }

    static AccessFilter<Class<?>> create(final Method method) {

        Class<?> declaringClass = method.getDeclaringClass();

        final ClassAccessFilter classLevelFilter =
                create(AccessLevel.get(declaringClass.getModifiers()), declaringClass);

        final ClassAccessFilter methodLevelFilter =
                Internal.getClassAccessFilterForAccessLevel(AccessLevel
                        .get(method.getModifiers()), declaringClass);

        if (classLevelFilter.isPublic() && methodLevelFilter.isPublic()) {
            return Internal.getClassAccessFilterForAccessLevel(AccessLevel.PUBLIC, declaringClass);
        }

        return new AccessFilter<Class<?>>() {

            @Override public AnnotatedElement filteredElement() {
                return method;
            }

            @Override public boolean isAccessibleTo(Class<?> cls) {
                return classLevelFilter.isAccessibleTo(cls) && methodLevelFilter.isAccessibleTo(cls);
            }

        };

    }

    static ClassAccessFilter create(Class<?> cls) {
        return create(AccessLevel.get(cls.getModifiers()), cls);
    }

    static ClassAccessFilter create(AccessLevel clsAccessLevel, final Class<?> cls) {

        if (clsAccessLevel == AccessLevel.UNDEFINED) {
            return create(cls);
        }

        if (cls.isLocalClass()) {
            return new ClassAccessFilter() {

                @Override public boolean isPublic() {
                    return false;
                }

                @Override public AnnotatedElement filteredElement() {
                    return cls;
                }

                @Override public boolean isAccessibleTo(Class<?> cls) {
                    return false;
                }
            };
        }

        Class<?> enclosingClass = cls.getEnclosingClass();

        if (enclosingClass == null) {
            return Internal.getClassAccessFilterForAccessLevel(clsAccessLevel, cls);
        }

        final ClassAccessFilter outerFilter = create(AccessLevel.get(enclosingClass.getModifiers()), enclosingClass);

        final ClassAccessFilter innerFilter =
                Internal.getClassAccessFilterForAccessLevel(clsAccessLevel, enclosingClass);

        if (outerFilter.isPublic() && innerFilter.isPublic()) {
            return Internal.getClassAccessFilterForAccessLevel(AccessLevel.PUBLIC, cls);
        }

        return new ClassAccessFilter() {

            @Override public boolean isPublic() {
                return false;
            }

            @Override public AnnotatedElement filteredElement() {
                return cls;
            }

            @Override public boolean isAccessibleTo(Class<?> cls) {
                return outerFilter.isAccessibleTo(cls) && innerFilter.isAccessibleTo(cls);
            }
        };

    }

    abstract class Internal {

        private static Class<?> getHighestEnclosingClass(Class<?> cls) {

            Class<?> enclosingClass = cls.getEnclosingClass();

            if (enclosingClass == null) {
                return cls;
            }

            return getHighestEnclosingClass(enclosingClass);

        }

        private static ClassAccessFilter getClassAccessFilterForAccessLevel(
                AccessLevel accessLevel, final Class<?> classOfResourceBeingRequested) {

            switch (accessLevel) {

                case PRIVATE: {

                    final Class<?> resourceEnclosingClass =
                            getHighestEnclosingClass(classOfResourceBeingRequested);

                    return new ClassAccessFilter() {

                        @Override public boolean isPublic() {
                            return false;
                        }

                        @Override public AnnotatedElement filteredElement() {
                            return classOfResourceBeingRequested;
                        }

                        @Override public boolean isAccessibleTo(final Class<?> classOfResourceRequestingAccess) {
                            return classOfResourceBeingRequested == classOfResourceRequestingAccess
                                    || resourceEnclosingClass == getHighestEnclosingClass(classOfResourceRequestingAccess);
                        }

                    };
                }

                case PROTECTED: {

                    final Package packageOfResourceBeingRequested = classOfResourceBeingRequested.getPackage();

                    return new ClassAccessFilter() {

                        @Override public boolean isPublic() {
                            return false;
                        }

                        @Override public AnnotatedElement filteredElement() {
                            return classOfResourceBeingRequested;
                        }

                        @Override public boolean isAccessibleTo(Class<?> classOfResourceRequestingAccess) {
                            return (packageOfResourceBeingRequested == classOfResourceRequestingAccess.getPackage())
                                    || classOfResourceBeingRequested.isAssignableFrom(classOfResourceRequestingAccess);
                        }

                    };

                }

                case PACKAGE_PRIVATE: {

                    final Package packageOfResourceBeingRequested = classOfResourceBeingRequested.getPackage();

                    return new ClassAccessFilter() {

                        @Override public boolean isPublic() {
                            return false;
                        }

                        @Override public AnnotatedElement filteredElement() {
                            return classOfResourceBeingRequested;
                        }

                        @Override public boolean isAccessibleTo(Class<?> classOfResourceRequestingAccess) {
                            return packageOfResourceBeingRequested == classOfResourceRequestingAccess.getPackage();
                        }

                    };

                }

                case PUBLIC: {

                    return new ClassAccessFilter() {

                        @Override public boolean isPublic() {
                            return true;
                        }

                        @Override public AnnotatedElement filteredElement() {
                            return classOfResourceBeingRequested;
                        }

                        @Override public boolean isAccessibleTo(Class<?> classOfResourceRequestingAccess) {
                            return true;
                        }

                    };

                }

                case UNDEFINED:
                default: {

                    throw new UnsupportedOperationException("AccessLevel.UNDEFINED should be used only as a placeholder");

                }

            }
        }

    }



}
