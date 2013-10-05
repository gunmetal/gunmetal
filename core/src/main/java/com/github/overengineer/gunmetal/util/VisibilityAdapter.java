package com.github.overengineer.gunmetal.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author rees.byars
 */
public interface VisibilityAdapter {

    boolean isPublic();

    boolean isVisibleTo(Class<?> cls);

    enum AccessLevel {

        PRIVATE {
            @Override
            public  VisibilityAdapter newVisibilityAdapter(final Class<?> classOfResourceBeingRequested) {
                return new VisibilityAdapter() {

                    @Override
                    public boolean isPublic() {
                        return false;
                    }

                    @Override
                    public boolean isVisibleTo(Class<?> classOfResourceRequestingAccess) {
                        return classOfResourceBeingRequested == classOfResourceRequestingAccess;
                    }

                };
            }
        },

        PACKAGE_PRIVATE {
            @Override
            public  VisibilityAdapter newVisibilityAdapter(final Class<?> classOfResourceBeingRequested) {
                return new VisibilityAdapter() {

                    @Override
                    public boolean isPublic() {
                        return false;
                    }

                    @Override
                    public boolean isVisibleTo(Class<?> classOfResourceRequestingAccess) {
                        return classOfResourceBeingRequested.getPackage() == classOfResourceRequestingAccess.getPackage();
                    }

                };
            }
        },

        PROTECTED {
            @Override
            public  VisibilityAdapter newVisibilityAdapter(final Class<?> classOfResourceBeingRequested) {
                return new VisibilityAdapter() {

                    @Override
                    public boolean isPublic() {
                        return false;
                    }

                    @Override
                    public boolean isVisibleTo(Class<?> classOfResourceRequestingAccess) {
                        return classOfResourceBeingRequested.getPackage() == classOfResourceRequestingAccess.getPackage();
                    }

                };
            }
        },

        PUBLIC {
            @Override
            public  VisibilityAdapter newVisibilityAdapter(final Class<?> classOfResourceBeingRequested) {
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

        abstract  VisibilityAdapter newVisibilityAdapter(Class<?>  cls);

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

        public static  VisibilityAdapter getAdapter(Method method) {

            Class<?> declaringClass = method.getDeclaringClass();

            final VisibilityAdapter classLevelAdapter =
                    AccessLevel
                            .get(declaringClass.getModifiers())
                            .newVisibilityAdapter(declaringClass);

            final VisibilityAdapter methodLevelAdapter =
                    AccessLevel
                            .get(method.getModifiers())
                            .newVisibilityAdapter(declaringClass);

            return new VisibilityAdapter() {

                @Override
                public boolean isPublic() {
                    return classLevelAdapter.isPublic() && methodLevelAdapter.isPublic();
                }

                @Override
                public boolean isVisibleTo(Class<?> cls) {
                    return classLevelAdapter.isVisibleTo(cls) && methodLevelAdapter.isVisibleTo(cls);
                }

            };

        }

        public static VisibilityAdapter getAdapter(Class cls) {

            Class<?> declaringClass = cls.getDeclaringClass();

            if (declaringClass == null) {
                return AccessLevel
                        .get(cls.getModifiers())
                        .newVisibilityAdapter(cls);
            }

            final VisibilityAdapter outerAdapter =
                    AccessLevel
                            .get(declaringClass.getModifiers())
                            .newVisibilityAdapter(declaringClass);

            final VisibilityAdapter innerAdapter =
                    AccessLevel
                            .get(cls.getModifiers())
                            .newVisibilityAdapter(declaringClass);

            return new VisibilityAdapter() {

                @Override
                public boolean isPublic() {
                    return outerAdapter.isPublic() && innerAdapter.isPublic();
                }

                @Override
                public boolean isVisibleTo(Class<?> cls) {
                    return outerAdapter.isVisibleTo(cls) && innerAdapter.isVisibleTo(cls);
                }

            };

        }

    }
}
