package io.gunmetal.internal;

import io.gunmetal.Gunmetal;
import io.gunmetal.testmocks.VisibilityTestBean;
import io.gunmetal.testmocks.VisibilityTestBean2;
import org.junit.Test;

import java.lang.reflect.Method;

import static io.gunmetal.internal.AccessFilterTest.AccessFilterTestingUtil.assertClass;
import static io.gunmetal.internal.AccessFilterTest.AccessFilterTestingUtil.assertPackagePrivateMethodOn;
import static io.gunmetal.internal.AccessFilterTest.AccessFilterTestingUtil.assertPrivateMethodOn;
import static io.gunmetal.internal.AccessFilterTest.AccessFilterTestingUtil.assertProtectedMethodOn;
import static io.gunmetal.internal.AccessFilterTest.AccessFilterTestingUtil.assertPublicMethodOn;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author rees.byars
 */
public class AccessFilterTest {

    @Test
    public void testGetMethodAdapter_public() {

        assertPublicMethodOn(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.class)
                .isVisibleTo(AccessFilter.class)
                .isVisibleTo(Gunmetal.class);
    }

    @Test
    public void testGetMethodAdapter_private() {

        assertPrivateMethodOn(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.class)
                .isNotVisibleTo(AccessFilter.class)
                .isNotVisibleTo(Gunmetal.class);
    }

    @Test
    public void testGetMethodAdapter_packagePrivate() {
        assertPackagePrivateMethodOn(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.class)
                .isVisibleTo(VisibilityTestBean2.class)
                .isNotVisibleTo(Gunmetal.class);
    }

    @Test
    public void testGetMethodAdapter_protected() {
        assertProtectedMethodOn(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean2.getPrivate())
                .isNotVisibleTo(AccessFilter.class)
                .isNotVisibleTo(Gunmetal.class)
                .isNotVisibleTo(new VisibilityTestBean() { }.getClass())
                .isVisibleTo(new VisibilityTestBean.PublicClass() { }.getClass());
    }

    @Test
    public void testGetMethodAdapter_local() {

        class Local {
            public void publicMethod() { }
        }

        assertPublicMethodOn(Local.class)
                .isNotVisibleTo(VisibilityTestBean2.getPrivate())
                .isNotVisibleTo(AccessFilter.class)
                .isNotVisibleTo(Gunmetal.class)
                .isNotVisibleTo(getClass());
    }

    @Test
    public void testGetClassAdapter_public() {
        assertClass(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.class)
                .isVisibleTo(AccessFilter.class)
                .isVisibleTo(Gunmetal.class);
    }

    @Test
    public void testGetClassAdapter_private() {
        assertClass(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isNotVisibleTo(VisibilityTestBean2.class)
                .isNotVisibleTo(AccessFilter.class)
                .isNotVisibleTo(Gunmetal.class);
    }

    @Test
    public void testGetClassAdapter_packagePrivate() {
        assertClass(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.class)
                .isVisibleTo(VisibilityTestBean2.class)
                .isNotVisibleTo(Gunmetal.class);
    }

    @Test
    public void testGetClassAdapter_protected() {
        assertClass(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean2.getPrivate())
                .isNotVisibleTo(AccessFilter.class)
                .isNotVisibleTo(Gunmetal.class)
                .isVisibleTo(new VisibilityTestBean() { }.getClass())
                .isNotVisibleTo(new VisibilityTestBean.PublicClass() { }.getClass());
    }

    @Test
    public void testGetClassAdapter_local() {

        class Local { }

        assertClass(Local.class)
                .isNotVisibleTo(VisibilityTestBean2.getPrivate())
                .isNotVisibleTo(AccessFilter.class)
                .isNotVisibleTo(Gunmetal.class)
                .isNotVisibleTo(getClass());
    }

    @Test
    public void testEdges() {

        assertClass(VisibilityTestBean.getPrivatePublic())
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.class)
                .isNotVisibleTo(AccessFilter.class)
                .isNotVisibleTo(Gunmetal.class);

        assertClass(VisibilityTestBean.getPackagePrivatePublic())
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.class)
                .isVisibleTo(VisibilityTestBean2.class)
                .isNotVisibleTo(Gunmetal.class);

        assertClass(VisibilityTestBean.getProtectedPublic())
                .isVisibleTo(VisibilityTestBean2.getPrivate())
                .isVisibleTo(new VisibilityTestBean() { }.getClass());

        assertClass(VisibilityTestBean.PublicClass.getProtected())
                .isVisibleTo(new VisibilityTestBean.PublicClass() { }.getClass())
                .isNotVisibleTo(new VisibilityTestBean() { }.getClass());

    }

    public static class AccessFilterTestingUtil {

        private static Method getPublicMethod(Class<?> cls) {
            return getMethod("publicMethod", cls);
        }

        private static Method getPrivateMethod(Class<?> cls) {
            return getMethod("privateMethod", cls);
        }

        private static Method getProtectedMethod(Class<?> cls) {
            return getMethod("protectedMethod", cls);
        }

        private static Method getPackagePrivateMethod(Class<?> cls) {
            return getMethod("packagePrivateMethod", cls);
        }

        private static Method getMethod(String name, Class<?> cls) {
            try {
                return cls.getDeclaredMethod(name);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public static AccessAssertion assertClass(Class<?> cls) {
            return new AccessAssertion(AccessFilter.Factory.getAccessFilter(cls));
        }

        public static AccessAssertion assertPrivateMethodOn(Class<?> cls) {
            return new AccessAssertion(AccessFilter.Factory.getAccessFilter(getPrivateMethod(cls)));
        }

        public static AccessAssertion assertPublicMethodOn(Class<?> cls) {
            return new AccessAssertion(AccessFilter.Factory.getAccessFilter(getPublicMethod(cls)));
        }

        public static AccessAssertion assertProtectedMethodOn(Class<?> cls) {
            return new AccessAssertion(AccessFilter.Factory.getAccessFilter(getProtectedMethod(cls)));
        }

        public static AccessAssertion assertPackagePrivateMethodOn(Class<?> cls) {
            return new AccessAssertion(AccessFilter.Factory.getAccessFilter(getPackagePrivateMethod(cls)));
        }

        public static class AccessAssertion {

            private AccessFilter<Class<?>> visibilityAdapter;

            AccessAssertion(AccessFilter<Class<?>> visibilityAdapter) {
                this.visibilityAdapter = visibilityAdapter;
            }

            public AccessAssertion isVisibleTo(Class<?> cls) {
                assertTrue("failed on " + cls.toString(), visibilityAdapter.isAccessibleTo(cls));
                return this;
            }

            public AccessAssertion isNotVisibleTo(Class<?> cls) {
                assertFalse("failed on " + cls.toString(), visibilityAdapter.isAccessibleTo(cls));
                return this;
            }

        }

    }

}
