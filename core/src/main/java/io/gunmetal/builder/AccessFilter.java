package io.gunmetal.builder;

import io.gunmetal.AccessLevel;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface AccessFilter<T> {

    // TODO turn this into an actual filter that is applied to ComponentAdapter.get ?
    // TODO make the filter return a report with a boolean and an error explanation in case of access failure ?
    // TODO then the

    boolean isAccessibleFrom(T target);

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
                public boolean isAccessibleFrom(Class<?> cls) {
                    return classLevelFilter.isAccessibleFrom(cls) && methodLevelFilter.isAccessibleFrom(cls);
                }

            };

        }

        static AccessFilter<Class<?>> getAccessFilter(Class<?> cls) {
            return getFilter(AccessLevel.get(cls.getModifiers()), cls);
        }

        static AccessFilter<Class<?>> getAccessFilter(AccessLevel accessLevel, Class<?> cls) {
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
                    public boolean isAccessibleFrom(Class<?> cls) {
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
