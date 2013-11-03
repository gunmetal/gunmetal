package io.gunmetal.builder;

import io.gunmetal.AccessLevel;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
interface VisibilityAdapter<T> {

    boolean isVisibleTo(T target);

    class Factory {

        private interface ClassVisibilityAdapter extends VisibilityAdapter<Class<?>> {
            boolean isPublic();
        }

        static VisibilityAdapter<Class<?>> getVisibilityAdapter(Method method) {

            Class<?> declaringClass = method.getDeclaringClass();

            final ClassVisibilityAdapter classLevelAdapter = getAdapter(declaringClass);

            final ClassVisibilityAdapter methodLevelAdapter =
                    getClassVisibilityAdapterForAccessLevel(AccessLevel
                            .get(method.getModifiers()), declaringClass);

            if (classLevelAdapter.isPublic() && methodLevelAdapter.isPublic()) {
                return getClassVisibilityAdapterForAccessLevel(AccessLevel.PUBLIC, declaringClass);
            }

            return new VisibilityAdapter<Class<?>>() {

                @Override
                public boolean isVisibleTo(Class<?> cls) {
                    return classLevelAdapter.isVisibleTo(cls) && methodLevelAdapter.isVisibleTo(cls);
                }

            };

        }

        static VisibilityAdapter<Class<?>> getVisibilityAdapter(Class<?> cls) {
            return getAdapter(cls);
        }

        private static ClassVisibilityAdapter getAdapter(Class<?> cls) {

            if (cls.isLocalClass()) {
                return new ClassVisibilityAdapter() {

                    @Override
                    public boolean isPublic() {
                        return false;
                    }

                    @Override
                    public boolean isVisibleTo(Class<?> cls) {
                        return false;
                    }
                };
            }

            Class<?> enclosingClass = cls.getEnclosingClass();

            if (enclosingClass == null) {
                return getClassVisibilityAdapterForAccessLevel(AccessLevel.get(cls.getModifiers()), cls);
            }

            final ClassVisibilityAdapter outerAdapter = getAdapter(enclosingClass);

            final ClassVisibilityAdapter innerAdapter =
                    getClassVisibilityAdapterForAccessLevel(AccessLevel.get(cls.getModifiers()), enclosingClass);

            if (outerAdapter.isPublic() && innerAdapter.isPublic()) {
                return getClassVisibilityAdapterForAccessLevel(AccessLevel.PUBLIC, cls);
            }

            return new ClassVisibilityAdapter() {

                @Override
                public boolean isPublic() {
                    return false;
                }

                @Override
                public boolean isVisibleTo(Class<?> cls) {
                    return outerAdapter.isVisibleTo(cls) && innerAdapter.isVisibleTo(cls);
                }
            };

        }

        static ClassVisibilityAdapter getClassVisibilityAdapterForAccessLevel(AccessLevel accessLevel, final Class<?> classOfResourceBeingRequested) {

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

                    return new ClassVisibilityAdapter() {

                        @Override
                        public boolean isPublic() {
                            return false;
                        }

                        @Override
                        public boolean isVisibleTo(final Class<?> classOfResourceRequestingAccess) {
                            return classOfResourceBeingRequested == classOfResourceRequestingAccess
                                    || resourceEnclosingClass ==new EnclosingUtil().getHighestEnclosingClass(classOfResourceRequestingAccess);
                        }

                    };
                }

                case PROTECTED: {

                    final Package packageOfResourceBeingRequested = classOfResourceBeingRequested.getPackage();

                    return new ClassVisibilityAdapter() {

                        @Override
                        public boolean isPublic() {
                            return false;
                        }

                        @Override
                        public boolean isVisibleTo(Class<?> classOfResourceRequestingAccess) {
                            return (packageOfResourceBeingRequested == classOfResourceRequestingAccess.getPackage())
                                    || classOfResourceBeingRequested.isAssignableFrom(classOfResourceRequestingAccess);
                        }

                    };

                }

                case PACKAGE_PRIVATE: {

                    final Package packageOfResourceBeingRequested = classOfResourceBeingRequested.getPackage();

                    return new ClassVisibilityAdapter() {

                        @Override
                        public boolean isPublic() {
                            return false;
                        }

                        @Override
                        public boolean isVisibleTo(Class<?> classOfResourceRequestingAccess) {
                            return packageOfResourceBeingRequested == classOfResourceRequestingAccess.getPackage();
                        }

                    };

                }

                case PUBLIC: {

                    return new ClassVisibilityAdapter() {

                        @Override
                        public boolean isPublic() {
                            return true;
                        }

                        @Override
                        public boolean isVisibleTo(Class<?> classOfResourceRequestingAccess) {
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
