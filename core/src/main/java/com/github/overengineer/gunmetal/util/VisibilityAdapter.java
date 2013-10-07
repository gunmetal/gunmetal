package com.github.overengineer.gunmetal.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author rees.byars
 */
public interface VisibilityAdapter {

    boolean isPublic();

    boolean isVisibleTo(Class<?> cls);

    enum AccessLevel implements Factory.AdapterProvider {

        PRIVATE {

            Class<?> getHighestEnclosingClose(Class<?> cls) {

                Class<?> enclosingClass = cls.getEnclosingClass();

                if (enclosingClass == null) {
                    return cls;
                }

                return getHighestEnclosingClose(enclosingClass);

            }

            @Override
            public VisibilityAdapter newVisibilityAdapter(final Class<?> classOfResourceBeingRequested) {

                final Class<?> resourceEnclosingClass = getHighestEnclosingClose(classOfResourceBeingRequested);

                return new VisibilityAdapter() {

                    @Override
                    public boolean isPublic() {
                        return false;
                    }

                    @Override
                    public boolean isVisibleTo(final Class<?> classOfResourceRequestingAccess) {
                        return classOfResourceBeingRequested == classOfResourceRequestingAccess
                                || resourceEnclosingClass == getHighestEnclosingClose(classOfResourceRequestingAccess);
                    }

                };
            }
        },

        PACKAGE_PRIVATE {
            @Override
            public VisibilityAdapter newVisibilityAdapter(Class<?> classOfResourceBeingRequested) {

                final Package packageOfResourceBeingRequested = classOfResourceBeingRequested.getPackage();

                return new VisibilityAdapter() {

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
        },

        PROTECTED {
            @Override
            public VisibilityAdapter newVisibilityAdapter(final Class<?> classOfResourceBeingRequested) {

                final Package packageOfResourceBeingRequested = classOfResourceBeingRequested.getPackage();

                return new VisibilityAdapter() {

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
        },

        PUBLIC {
            @Override
            public VisibilityAdapter newVisibilityAdapter(final Class<?> classOfResourceBeingRequested) {
                return new VisibilityAdapter() {

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
        };

        static AccessLevel get(int modifiers) {
            if (Modifier.isPublic(modifiers)) {
                return PUBLIC;
            } else if (Modifier.isPrivate(modifiers)) {
                return PRIVATE;
            } else if (Modifier.isProtected(modifiers)) {
                return PROTECTED;
            } else {
                return PACKAGE_PRIVATE;
            }
        }
    }

    class Factory {

        private interface AdapterProvider {
            VisibilityAdapter newVisibilityAdapter(Class<?>  cls);
        }

        public static VisibilityAdapter getAdapter(Method method) {

            Class<?> declaringClass = method.getDeclaringClass();

            final VisibilityAdapter classLevelAdapter = getAdapter(declaringClass);

            final VisibilityAdapter methodLevelAdapter =
                    AccessLevel
                            .get(method.getModifiers())
                            .newVisibilityAdapter(declaringClass);

            if (classLevelAdapter.isPublic() && methodLevelAdapter.isPublic()) {
                return AccessLevel.PUBLIC.newVisibilityAdapter(declaringClass);
            }

            return new VisibilityAdapter() {

                @Override
                public boolean isPublic() {
                    return false;
                }

                @Override
                public boolean isVisibleTo(Class<?> cls) {
                    return classLevelAdapter.isVisibleTo(cls) && methodLevelAdapter.isVisibleTo(cls);
                }

            };

        }

        public static VisibilityAdapter getAdapter(Class cls) {

            if (cls.isLocalClass()) {
                return new VisibilityAdapter() {
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
                return AccessLevel
                        .get(cls.getModifiers())
                        .newVisibilityAdapter(cls);
            }

            final VisibilityAdapter outerAdapter = getAdapter(enclosingClass);

            final VisibilityAdapter innerAdapter =
                    AccessLevel
                            .get(cls.getModifiers())
                            .newVisibilityAdapter(enclosingClass);

            if (outerAdapter.isPublic() && innerAdapter.isPublic()) {
                return AccessLevel.PUBLIC.newVisibilityAdapter(cls);
            }

            return new VisibilityAdapter() {

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

    }
}
