package io.gunmetal.builder;

import io.gunmetal.AccessLevel;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface AccessFilter<T> {

    boolean isAccessibleTo(T target);

    class Factory {

        private interface ClassAccessFilter extends AccessFilter<Class<?>> {
            boolean isPublic();
        }

        static AccessFilter<Class<?>> getAccessFilter(Method method) {

            Class<?> declaringClass = method.getDeclaringClass();

            final ClassAccessFilter classLevelFilter =
                    getFilter(AccessLevel.get(declaringClass.getModifiers()), declaringClass);

            final ClassAccessFilter methodLevelFilter =
                    getClassAccessFilterForAccessLevel(AccessLevel
                            .get(method.getModifiers()), declaringClass);

            if (classLevelFilter.isPublic() && methodLevelFilter.isPublic()) {
                return getClassAccessFilterForAccessLevel(AccessLevel.PUBLIC, declaringClass);
            }

            return new AccessFilter<Class<?>>() {

                @Override
                public boolean isAccessibleTo(Class<?> cls) {
                    return classLevelFilter.isAccessibleTo(cls) && methodLevelFilter.isAccessibleTo(cls);
                }

            };

        }

        static AccessFilter<Class<?>> getAccessFilter(Class<?> cls) {
            return getFilter(AccessLevel.get(cls.getModifiers()), cls);
        }

        static AccessFilter<Class<?>> getAccessFilter(AccessLevel accessLevel, Class<?> cls) {
            if (accessLevel == AccessLevel.UNDEFINED) {
                return getAccessFilter(cls);
            }
            return getFilter(accessLevel, cls);
        }

        private static ClassAccessFilter getFilter(AccessLevel clsAccessLevel, Class<?> cls) {

            if (cls.isLocalClass()) {
                return new ClassAccessFilter() {

                    @Override
                    public boolean isPublic() {
                        return false;
                    }

                    @Override
                    public boolean isAccessibleTo(Class<?> cls) {
                        return false;
                    }
                };
            }

            Class<?> enclosingClass = cls.getEnclosingClass();

            if (enclosingClass == null) {
                return getClassAccessFilterForAccessLevel(clsAccessLevel, cls);
            }

            final ClassAccessFilter outerFilter = getFilter(AccessLevel.get(enclosingClass.getModifiers()), enclosingClass);

            final ClassAccessFilter innerFilter =
                    getClassAccessFilterForAccessLevel(clsAccessLevel, enclosingClass);

            if (outerFilter.isPublic() && innerFilter.isPublic()) {
                return getClassAccessFilterForAccessLevel(AccessLevel.PUBLIC, cls);
            }

            return new ClassAccessFilter() {

                @Override
                public boolean isPublic() {
                    return false;
                }

                @Override
                public boolean isAccessibleTo(Class<?> cls) {
                    return outerFilter.isAccessibleTo(cls) && innerFilter.isAccessibleTo(cls);
                }
            };

        }

        private static ClassAccessFilter getClassAccessFilterForAccessLevel(AccessLevel accessLevel, final Class<?> classOfResourceBeingRequested) {

            switch (accessLevel) {

                case PRIVATE: {

                    class EnclosingUtil {

                        Class<?> getHighestEnclosingClass(Class<?> cls) {

                            Class<?> enclosingClass = cls.getEnclosingClass();

                            if (enclosingClass == null) {
                                return cls;
                            }

                            return getHighestEnclosingClass(enclosingClass);

                        }

                    }

                    final Class<?> resourceEnclosingClass = new EnclosingUtil().getHighestEnclosingClass(classOfResourceBeingRequested);

                    return new ClassAccessFilter() {

                        @Override
                        public boolean isPublic() {
                            return false;
                        }

                        @Override
                        public boolean isAccessibleTo(final Class<?> classOfResourceRequestingAccess) {
                            return classOfResourceBeingRequested == classOfResourceRequestingAccess
                                    || resourceEnclosingClass == new EnclosingUtil().getHighestEnclosingClass(classOfResourceRequestingAccess);
                        }

                    };
                }

                case PROTECTED: {

                    final Package packageOfResourceBeingRequested = classOfResourceBeingRequested.getPackage();

                    return new ClassAccessFilter() {

                        @Override
                        public boolean isPublic() {
                            return false;
                        }

                        @Override
                        public boolean isAccessibleTo(Class<?> classOfResourceRequestingAccess) {
                            return (packageOfResourceBeingRequested == classOfResourceRequestingAccess.getPackage())
                                    || classOfResourceBeingRequested.isAssignableFrom(classOfResourceRequestingAccess);
                        }

                    };

                }

                case PACKAGE_PRIVATE: {

                    final Package packageOfResourceBeingRequested = classOfResourceBeingRequested.getPackage();

                    return new ClassAccessFilter() {

                        @Override
                        public boolean isPublic() {
                            return false;
                        }

                        @Override
                        public boolean isAccessibleTo(Class<?> classOfResourceRequestingAccess) {
                            return packageOfResourceBeingRequested == classOfResourceRequestingAccess.getPackage();
                        }

                    };

                }

                case PUBLIC: {

                    return new ClassAccessFilter() {

                        @Override
                        public boolean isPublic() {
                            return true;
                        }

                        @Override
                        public boolean isAccessibleTo(Class<?> classOfResourceRequestingAccess) {
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
