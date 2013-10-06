package com.github.overengineer.gunmetal.util;

import com.github.overengineer.gunmetal.Gunmetal;
import com.github.overengineer.gunmetal.util.visibilityadaptertest.VisibilityTestBean;
import com.github.overengineer.gunmetal.util.visibilityadaptertest.VisibilityTestBean2;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author rees.byars
 */
public class VisibilityAdapterTest {

    public static class PublicClass {

        public void publicMethod() { }
        protected void protectedMethod() { }
        private void privateMethod() { }
        void packagePrivateMethod() { }

        protected static class ProtectedClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        private static class PrivateClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        static class PackagePrivateClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

    }

    protected static class ProtectedClass {

        public void publicMethod() { }
        protected void protectedMethod() { }
        private void privateMethod() { }
        void packagePrivateMethod() { }

        public static class PublicClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        private static class PrivateClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        static class PackagePrivateClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

    }

    private static class PrivateClass {

        public void publicMethod() { }
        protected void protectedMethod() { }
        private void privateMethod() { }
        void packagePrivateMethod() { }

        public static class PublicClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        protected static class ProtectedClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        static class PackagePrivateClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

    }

    static class PackagePrivateClass {

        public void publicMethod() { }
        protected void protectedMethod() { }
        private void privateMethod() { }
        void packagePrivateMethod() { }

        public static class PublicClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        protected static class ProtectedClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

        private static class PrivateClass {
            public void publicMethod() { }
            protected void protectedMethod() { }
            private void privateMethod() { }
            void packagePrivateMethod() { }
        }

    }

    public static Method getPublicMethod(Class<?> cls) {
        return getMethod("publicMethod", cls);
    }

    public static Method getPrivateMethod(Class<?> cls) {
        return getMethod("privateMethod", cls);
    }

    public static Method getProtectedMethod(Class<?> cls) {
        return getMethod("protectedMethod", cls);
    }

    public static Method getPackagePrivateMethod(Class<?> cls) {
        return getMethod("packagePrivateMethod", cls);
    }

    private static Method getMethod(String name, Class<?> cls) {
        try {
            return cls.getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetMethodAdapter_public() {
        VisibilityAdapter publicPublic = VisibilityAdapter.Factory.getAdapter(getPublicMethod(PublicClass.class));
        assertTrue(publicPublic.isPublic());
        assertTrue(publicPublic.isVisibleTo(PrivateClass.class));
        assertTrue(publicPublic.isVisibleTo(ProtectedClass.class));
        assertTrue(publicPublic.isVisibleTo(PackagePrivateClass.class));
        assertTrue(publicPublic.isVisibleTo(PublicClass.class));
        assertTrue(publicPublic.isVisibleTo(VisibilityAdapterTest.class));
        assertTrue(publicPublic.isVisibleTo(VisibilityAdapter.class));
        assertTrue(publicPublic.isVisibleTo(Gunmetal.class));
    }

    @Test
    public void testGetMethodAdapter_private() {
        VisibilityAdapter publicPrivate = VisibilityAdapter.Factory.getAdapter(getPrivateMethod(PublicClass.class));
        assertFalse(publicPrivate.isPublic());
        assertTrue(publicPrivate.isVisibleTo(PrivateClass.class));
        assertTrue(publicPrivate.isVisibleTo(ProtectedClass.class));
        assertTrue(publicPrivate.isVisibleTo(PackagePrivateClass.class));
        assertTrue(publicPrivate.isVisibleTo(PublicClass.class));
        assertTrue(publicPrivate.isVisibleTo(VisibilityAdapterTest.class));
        assertFalse(publicPrivate.isVisibleTo(VisibilityAdapter.class));
        assertFalse(publicPrivate.isVisibleTo(Gunmetal.class));
    }

    @Test
    public void testGetMethodAdapter_packagePrivate() {
        VisibilityAdapter publicPackagePrivate = VisibilityAdapter.Factory.getAdapter(getPackagePrivateMethod(PublicClass.class));
        assertFalse(publicPackagePrivate.isPublic());
        assertTrue(publicPackagePrivate.isVisibleTo(PrivateClass.class));
        assertTrue(publicPackagePrivate.isVisibleTo(ProtectedClass.class));
        assertTrue(publicPackagePrivate.isVisibleTo(PackagePrivateClass.class));
        assertTrue(publicPackagePrivate.isVisibleTo(PublicClass.class));
        assertTrue(publicPackagePrivate.isVisibleTo(VisibilityAdapterTest.class));
        assertTrue(publicPackagePrivate.isVisibleTo(VisibilityAdapter.class));
        assertFalse(publicPackagePrivate.isVisibleTo(Gunmetal.class));
    }

    @Test
    public void testGetMethodAdapter_protected() {
        VisibilityAdapter publicProtected = VisibilityAdapter.Factory.getAdapter(getProtectedMethod(VisibilityTestBean.PublicClass.class));
        assertFalse(publicProtected.isPublic());
        assertFalse(publicProtected.isVisibleTo(PrivateClass.class));
        assertFalse(publicProtected.isVisibleTo(ProtectedClass.class));
        assertFalse(publicProtected.isVisibleTo(PackagePrivateClass.class));
        assertFalse(publicProtected.isVisibleTo(PublicClass.class));
        assertFalse(publicProtected.isVisibleTo(VisibilityAdapterTest.class));
        assertFalse(publicProtected.isVisibleTo(VisibilityAdapter.class));
        assertFalse(publicProtected.isVisibleTo(Gunmetal.class));
        assertFalse(publicProtected.isVisibleTo(new VisibilityTestBean() { }.getClass()));
        assertTrue(publicProtected.isVisibleTo(new VisibilityTestBean.PublicClass() { }.getClass()));
    }

    @Test
    public void testGetClassAdapter_public() {
        VisibilityAdapter publicPublic = VisibilityAdapter.Factory.getAdapter(PublicClass.class);
        assertTrue(publicPublic.isPublic());
        assertTrue(publicPublic.isVisibleTo(PrivateClass.class));
        assertTrue(publicPublic.isVisibleTo(ProtectedClass.class));
        assertTrue(publicPublic.isVisibleTo(PackagePrivateClass.class));
        assertTrue(publicPublic.isVisibleTo(PublicClass.class));
        assertTrue(publicPublic.isVisibleTo(VisibilityAdapterTest.class));
        assertTrue(publicPublic.isVisibleTo(VisibilityAdapter.class));
        assertTrue(publicPublic.isVisibleTo(Gunmetal.class));
    }

    @Test
    public void testGetClassAdapter_private() {
        VisibilityAdapter publicPrivate = VisibilityAdapter.Factory.getAdapter(PrivateClass.class);
        assertFalse(publicPrivate.isPublic());
        assertTrue(publicPrivate.isVisibleTo(PrivateClass.class));
        assertTrue(publicPrivate.isVisibleTo(ProtectedClass.class));
        assertTrue(publicPrivate.isVisibleTo(PackagePrivateClass.class));
        assertTrue(publicPrivate.isVisibleTo(PublicClass.class));
        assertTrue(publicPrivate.isVisibleTo(VisibilityAdapterTest.class));
        assertFalse(publicPrivate.isVisibleTo(VisibilityAdapter.class));
        assertFalse(publicPrivate.isVisibleTo(Gunmetal.class));
    }

    @Test
    public void testGetClassAdapter_packagePrivate() {
        VisibilityAdapter publicPackagePrivate = VisibilityAdapter.Factory.getAdapter(PackagePrivateClass.class);
        assertFalse(publicPackagePrivate.isPublic());
        assertTrue(publicPackagePrivate.isVisibleTo(PrivateClass.class));
        assertTrue(publicPackagePrivate.isVisibleTo(ProtectedClass.class));
        assertTrue(publicPackagePrivate.isVisibleTo(PackagePrivateClass.class));
        assertTrue(publicPackagePrivate.isVisibleTo(PublicClass.class));
        assertTrue(publicPackagePrivate.isVisibleTo(VisibilityAdapterTest.class));
        assertTrue(publicPackagePrivate.isVisibleTo(VisibilityAdapter.class));
        assertFalse(publicPackagePrivate.isVisibleTo(Gunmetal.class));
    }

    @Test
    public void testGetClassAdapter_protected() {
        VisibilityAdapter publicProtected = VisibilityAdapter.Factory.getAdapter(VisibilityTestBean.getProtected());
        assertFalse(publicProtected.isPublic());
        assertFalse(publicProtected.isVisibleTo(PrivateClass.class));
        assertFalse(publicProtected.isVisibleTo(ProtectedClass.class));
        assertFalse(publicProtected.isVisibleTo(PackagePrivateClass.class));
        assertFalse(publicProtected.isVisibleTo(PublicClass.class));
        assertFalse(publicProtected.isVisibleTo(VisibilityAdapterTest.class));
        assertFalse(publicProtected.isVisibleTo(VisibilityAdapter.class));
        assertFalse(publicProtected.isVisibleTo(Gunmetal.class));
        assertTrue(publicProtected.isVisibleTo(new VisibilityTestBean() { }.getClass()));
        assertFalse(publicProtected.isVisibleTo(new VisibilityTestBean.PublicClass() { }.getClass()));
    }

    @Test
    public void testEdges() {

        VisibilityAdapter publicPrivatePublic = VisibilityAdapter.Factory.getAdapter(PrivateClass.PublicClass.class);
        assertFalse(publicPrivatePublic.isPublic());
        assertTrue(publicPrivatePublic.isVisibleTo(PrivateClass.class));
        assertTrue(publicPrivatePublic.isVisibleTo(ProtectedClass.class));
        assertTrue(publicPrivatePublic.isVisibleTo(PackagePrivateClass.class));
        assertTrue(publicPrivatePublic.isVisibleTo(PublicClass.class));
        assertTrue(publicPrivatePublic.isVisibleTo(VisibilityAdapterTest.class));
        assertFalse(publicPrivatePublic.isVisibleTo(VisibilityAdapter.class));
        assertFalse(publicPrivatePublic.isVisibleTo(Gunmetal.class));

        VisibilityAdapter publicPackagePrivatePublic = VisibilityAdapter.Factory.getAdapter(PackagePrivateClass.PublicClass.class);
        assertFalse(publicPackagePrivatePublic.isPublic());
        assertTrue(publicPackagePrivatePublic.isVisibleTo(PrivateClass.class));
        assertTrue(publicPackagePrivatePublic.isVisibleTo(ProtectedClass.class));
        assertTrue(publicPackagePrivatePublic.isVisibleTo(PackagePrivateClass.class));
        assertTrue(publicPackagePrivatePublic.isVisibleTo(PublicClass.class));
        assertTrue(publicPackagePrivatePublic.isVisibleTo(VisibilityAdapterTest.class));
        assertTrue(publicPackagePrivatePublic.isVisibleTo(VisibilityAdapter.class));
        assertFalse(publicPackagePrivatePublic.isVisibleTo(Gunmetal.class));

        VisibilityAdapter publicProtectedPublic = VisibilityAdapter.Factory.getAdapter(VisibilityTestBean.getProtectedPublic());
        assertFalse(publicProtectedPublic.isPublic());
        assertTrue(publicProtectedPublic.isVisibleTo(VisibilityTestBean2.getPrivate()));
        assertTrue(publicProtectedPublic.isVisibleTo(new VisibilityTestBean() { }.getClass()));

        VisibilityAdapter publicPublicProtected = VisibilityAdapter.Factory.getAdapter(VisibilityTestBean.PublicClass.getProtected());
        assertTrue(publicPublicProtected.isVisibleTo(new VisibilityTestBean.PublicClass() { }.getClass()));
        assertFalse(publicPublicProtected.isVisibleTo(new VisibilityTestBean() { }.getClass()));

    }

}
