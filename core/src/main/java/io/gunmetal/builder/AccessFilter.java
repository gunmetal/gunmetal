package io.gunmetal.builder;

import io.gunmetal.AccessLevel;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface AccessFilter<T> {

    boolean isAccessibleFrom(T target);

    class Factory {

        private interface ClassAccessFilter extends AccessFilter<Class<?>> {
            boolean isPublic();
        }

        static AccessFilter<Class<?>> getAccessFilter(Method method) {

            Class<?> declaringClass = method.getDeclaringClass();

            final ClassAccessFilter classLevelFilter = getFilter(declaringClass);

            final ClassAccessFilter methodLevelFilter =
                    getClassAccessFilterForAccessLevel(AccessLevel
                            .get(method.getModifiers()), declaringClass);

            if (classLevelFilter.isPublic() && methodLevelFilter.isPublic()) {
                return getClassAccessFilterForAccessLevel(AccessLevel.PUBLIC, declaringClass);
            }

            return new AccessFilter<Class<?>>() {

                @Override
                public boolean isAccessibleFrom(Class<?> cls) {
                    return classLevelFilter.isAccessibleFrom(cls) && methodLevelFilter.isAccessibleFrom(cls);
                }

            };

        }

        static AccessFilter<Class<?>> getAccessFilter(Class<?> cls) {
            return getFilter(cls);
        }

        private static ClassAccessFilter getFilter(Class<?> cls) {

            if (cls.isLocalClass()) {
                return new ClassAccessFilter() {

                    @Override
                    public boolean isPublic() {
                        return false;
                    }

                    @Override
                    public boolean isAccessibleFrom(Class<?> cls) {
                        return false;
                    }
                };
            }

            Class<?> enclosingClass = cls.getEnclosingClass();

            if (enclosingClass == null) {
                return getClassAccessFilterForAccessLevel(AccessLevel.get(cls.getModifiers()), cls);
            }

            final ClassAccessFilter outerFilter = getFilter(enclosingClass);

            final ClassAccessFilter innerFilter =
                    getClassAccessFilterForAccessLevel(AccessLevel.get(cls.getModifiers()), enclosingClass);

            if (outerFilter.isPublic() && innerFilter.isPublic()) {
                return getClassAccessFilterForAccessLevel(AccessLevel.PUBLIC, cls);
            }

            return new ClassAccessFilter() {

                @Override
                public boolean isPublic() {
                    return false;
                }

                @Override
                public boolean isAccessibleFrom(Class<?> cls) {
                    return outerFilter.isAccessibleFrom(cls) && innerFilter.isAccessibleFrom(cls);
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
                        public boolean isAccessibleFrom(final Class<?> classOfResourceRequestingAccess) {
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
                        public boolean isAccessibleFrom(Class<?> classOfResourceRequestingAccess) {
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
                        public boolean isAccessibleFrom(Class<?> classOfResourceRequestingAccess) {
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
                        public boolean isAccessibleFrom(Class<?> classOfResourceRequestingAccess) {
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
