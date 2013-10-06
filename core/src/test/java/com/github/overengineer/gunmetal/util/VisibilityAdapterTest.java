package com.github.overengineer.gunmetal.util;

import com.github.overengineer.gunmetal.Gunmetal;
import com.github.overengineer.gunmetal.util.visibilityadaptertest.VisibilityTestBean;
import com.github.overengineer.gunmetal.util.visibilityadaptertest.VisibilityTestBean2;
import org.junit.Test;

import static com.github.overengineer.gunmetal.util.visibilityadaptertest.VisibilityAdapterTestingUtil.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        VisibilityAdapter publicPrivate = VisibilityAdapter.Factory.getAdapter(VisibilityTestBean.getPrivate());
        assertFalse(publicPrivate.isPublic());
        assertTrue(publicPrivate.isVisibleTo(VisibilityTestBean.getPrivate()));
        assertTrue(publicPrivate.isVisibleTo(VisibilityTestBean.getProtected()));
        assertTrue(publicPrivate.isVisibleTo(VisibilityTestBean.getPackagePrivate()));
        assertTrue(publicPrivate.isVisibleTo(VisibilityTestBean.PublicClass.class));
        assertTrue(publicPrivate.isVisibleTo(VisibilityTestBean.class));
        assertFalse(publicPrivate.isVisibleTo(VisibilityAdapter.class));
        assertFalse(publicPrivate.isVisibleTo(Gunmetal.class));
    }

    @Test
    public void testGetClassAdapter_packagePrivate() {
        VisibilityAdapter publicPackagePrivate = VisibilityAdapter.Factory.getAdapter(VisibilityTestBean.getPackagePrivate());
        assertFalse(publicPackagePrivate.isPublic());
        assertTrue(publicPackagePrivate.isVisibleTo(VisibilityTestBean.getPrivate()));
        assertTrue(publicPackagePrivate.isVisibleTo(VisibilityTestBean.getProtected()));
        assertTrue(publicPackagePrivate.isVisibleTo(VisibilityTestBean.getPackagePrivate()));
        assertTrue(publicPackagePrivate.isVisibleTo(VisibilityTestBean.PublicClass.class));
        assertTrue(publicPackagePrivate.isVisibleTo(VisibilityTestBean.class));
        assertTrue(publicPackagePrivate.isVisibleTo(VisibilityTestBean2.class));
        assertFalse(publicPackagePrivate.isVisibleTo(Gunmetal.class));
    }

    @Test
    public void testGetClassAdapter_protected() {
        VisibilityAdapter publicProtected = VisibilityAdapter.Factory.getAdapter(VisibilityTestBean.getProtected());
        assertFalse(publicProtected.isPublic());
        assertTrue(publicProtected.isVisibleTo(VisibilityTestBean2.getPrivate()));
        assertFalse(publicProtected.isVisibleTo(VisibilityAdapter.class));
        assertFalse(publicProtected.isVisibleTo(Gunmetal.class));
        assertTrue(publicProtected.isVisibleTo(new VisibilityTestBean() { }.getClass()));
        assertFalse(publicProtected.isVisibleTo(new VisibilityTestBean.PublicClass() { }.getClass()));
    }

    @Test
    public void testEdges() {

        VisibilityAdapter publicPrivatePublic = VisibilityAdapter.Factory.getAdapter(VisibilityTestBean.getPrivatePublic());
        assertFalse(publicPrivatePublic.isPublic());
        assertTrue(publicPrivatePublic.isVisibleTo(VisibilityTestBean.getPrivate()));
        assertTrue(publicPrivatePublic.isVisibleTo(VisibilityTestBean.getProtected()));
        assertTrue(publicPrivatePublic.isVisibleTo(VisibilityTestBean.getPackagePrivate()));
        assertTrue(publicPrivatePublic.isVisibleTo(VisibilityTestBean.PublicClass.class));
        assertTrue(publicPrivatePublic.isVisibleTo(VisibilityTestBean.class));
        assertFalse(publicPrivatePublic.isVisibleTo(VisibilityAdapter.class));
        assertFalse(publicPrivatePublic.isVisibleTo(Gunmetal.class));

        VisibilityAdapter publicPackagePrivatePublic = VisibilityAdapter.Factory.getAdapter(VisibilityTestBean.getPackagePrivatePublic());
        assertFalse(publicPackagePrivatePublic.isPublic());
        assertTrue(publicPackagePrivatePublic.isVisibleTo(VisibilityTestBean.getPrivate()));
        assertTrue(publicPackagePrivatePublic.isVisibleTo(VisibilityTestBean.getProtected()));
        assertTrue(publicPackagePrivatePublic.isVisibleTo(VisibilityTestBean.getPackagePrivate()));
        assertTrue(publicPackagePrivatePublic.isVisibleTo(VisibilityTestBean.PublicClass.class));
        assertTrue(publicPackagePrivatePublic.isVisibleTo(VisibilityTestBean.class));
        assertTrue(publicPackagePrivatePublic.isVisibleTo(VisibilityTestBean2.class));
        assertFalse(publicPackagePrivatePublic.isVisibleTo(Gunmetal.class));

        VisibilityAdapter publicProtectedPublic = VisibilityAdapter.Factory.getAdapter(VisibilityTestBean.getProtectedPublic());
        assertFalse(publicProtectedPublic.isPublic());
        assertTrue(publicProtectedPublic.isVisibleTo(VisibilityTestBean2.getPrivate()));
        assertTrue(publicProtectedPublic.isVisibleTo(new VisibilityTestBean() { }.getClass()));

        assertClass(VisibilityTestBean.PublicClass.getProtected())
                .isVisibleTo(new VisibilityTestBean.PublicClass() { }.getClass())
                .isNotVisibleTo(new VisibilityTestBean() { }.getClass());

    }

}
