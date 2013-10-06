package com.github.overengineer.gunmetal.util.visibilityadaptertest;

import com.github.overengineer.gunmetal.util.VisibilityAdapter;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author rees.byars
 */
public class VisibilityAdapterTestingUtil {

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

    public static VisibilityAssertion assertClass(Class<?> cls) {
        return new VisibilityAssertion(VisibilityAdapter.Factory.getAdapter(cls));
    }

    public static VisibilityAssertion assertPrivateMethodOn(Class<?> cls) {
        return new VisibilityAssertion(VisibilityAdapter.Factory.getAdapter(getPrivateMethod(cls)));
    }

    public static VisibilityAssertion assertPublicMethodOn(Class<?> cls) {
        return new VisibilityAssertion(VisibilityAdapter.Factory.getAdapter(getPublicMethod(cls)));
    }

    public static VisibilityAssertion assertProtectedMethodOn(Class<?> cls) {
        return new VisibilityAssertion(VisibilityAdapter.Factory.getAdapter(getProtectedMethod(cls)));
    }

    public static VisibilityAssertion assertPackagePrivateMethodOn(Class<?> cls) {
        return new VisibilityAssertion(VisibilityAdapter.Factory.getAdapter(getPackagePrivateMethod(cls)));
    }

    public static class VisibilityAssertion {

        private VisibilityAdapter visibilityAdapter;

        VisibilityAssertion(VisibilityAdapter visibilityAdapter) {
            this.visibilityAdapter = visibilityAdapter;
        }

        public VisibilityAssertion isPublic() {
            assertTrue(visibilityAdapter.isPublic());
            return this;
        }

        public VisibilityAssertion isNotPublic() {
            assertFalse(visibilityAdapter.isPublic());
            return this;
        }

        public VisibilityAssertion isVisibleTo(Class<?> cls) {
            assertTrue(visibilityAdapter.isVisibleTo(cls));
            return this;
        }

        public VisibilityAssertion isNotVisibleTo(Class<?> cls) {
            assertFalse(visibilityAdapter.isVisibleTo(cls));
            return this;
        }

    }

}
