package com.github.overengineer.gunmetal.util;

import com.github.overengineer.gunmetal.Gunmetal;
import com.github.overengineer.gunmetal.util.visibilityadaptertest.VisibilityTestBean;
import com.github.overengineer.gunmetal.util.visibilityadaptertest.VisibilityTestBean2;
import org.junit.Test;

import static com.github.overengineer.gunmetal.util.visibilityadaptertest.VisibilityAdapterTestingUtil.*;

/**
 * @author rees.byars
 */
public class VisibilityAdapterTest {

    @Test
    public void testGetMethodAdapter_public() {

        assertPublicMethodOn(VisibilityTestBean.PublicClass.class)
                .isPublic()
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.class)
                .isVisibleTo(VisibilityAdapter.class)
                .isVisibleTo(Gunmetal.class);
    }

    @Test
    public void testGetMethodAdapter_private() {

        assertPrivateMethodOn(VisibilityTestBean.PublicClass.class)
                .isNotPublic()
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.class)
                .isNotVisibleTo(VisibilityAdapter.class)
                .isNotVisibleTo(Gunmetal.class);
    }

    @Test
    public void testGetMethodAdapter_packagePrivate() {
        assertPackagePrivateMethodOn(VisibilityTestBean.PublicClass.class)
                .isNotPublic()
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
                .isNotPublic()
                .isVisibleTo(VisibilityTestBean2.getPrivate())
                .isNotVisibleTo(VisibilityAdapter.class)
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
                .isNotPublic()
                .isNotVisibleTo(VisibilityTestBean2.getPrivate())
                .isNotVisibleTo(VisibilityAdapter.class)
                .isNotVisibleTo(Gunmetal.class)
                .isNotVisibleTo(getClass());
    }

    @Test
    public void testGetClassAdapter_public() {
        assertClass(VisibilityTestBean.PublicClass.class)
                .isPublic()
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.class)
                .isVisibleTo(VisibilityAdapter.class)
                .isVisibleTo(Gunmetal.class);
    }

    @Test
    public void testGetClassAdapter_private() {
        assertClass(VisibilityTestBean.getPrivate())
                .isNotPublic()
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isNotVisibleTo(VisibilityTestBean2.class)
                .isNotVisibleTo(VisibilityAdapter.class)
                .isNotVisibleTo(Gunmetal.class);
    }

    @Test
    public void testGetClassAdapter_packagePrivate() {
        assertClass(VisibilityTestBean.getPackagePrivate())
                .isNotPublic()
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
                .isNotPublic()
                .isVisibleTo(VisibilityTestBean2.getPrivate())
                .isNotVisibleTo(VisibilityAdapter.class)
                .isNotVisibleTo(Gunmetal.class)
                .isVisibleTo(new VisibilityTestBean() { }.getClass())
                .isNotVisibleTo(new VisibilityTestBean.PublicClass() { }.getClass());
    }

    @Test
    public void testGetClassAdapter_local() {

        class Local { }

        assertClass(Local.class)
                .isNotPublic()
                .isNotVisibleTo(VisibilityTestBean2.getPrivate())
                .isNotVisibleTo(VisibilityAdapter.class)
                .isNotVisibleTo(Gunmetal.class)
                .isNotVisibleTo(getClass());
    }

    @Test
    public void testEdges() {

        assertClass(VisibilityTestBean.getPrivatePublic())
                .isNotPublic()
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.class)
                .isNotVisibleTo(VisibilityAdapter.class)
                .isNotVisibleTo(Gunmetal.class);

        assertClass(VisibilityTestBean.getPackagePrivatePublic())
                .isNotPublic()
                .isVisibleTo(VisibilityTestBean.getPrivate())
                .isVisibleTo(VisibilityTestBean.getProtected())
                .isVisibleTo(VisibilityTestBean.getPackagePrivate())
                .isVisibleTo(VisibilityTestBean.PublicClass.class)
                .isVisibleTo(VisibilityTestBean.class)
                .isVisibleTo(VisibilityTestBean2.class)
                .isNotVisibleTo(Gunmetal.class);

        assertClass(VisibilityTestBean.getProtectedPublic())
                .isNotPublic()
                .isVisibleTo(VisibilityTestBean2.getPrivate())
                .isVisibleTo(new VisibilityTestBean() { }.getClass());

        assertClass(VisibilityTestBean.PublicClass.getProtected())
                .isVisibleTo(new VisibilityTestBean.PublicClass() { }.getClass())
                .isNotVisibleTo(new VisibilityTestBean() { }.getClass());

    }

}
