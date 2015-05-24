package io.gunmetal.integration;

import io.gunmetal.Module;
import io.gunmetal.MultiBind;
import io.gunmetal.Supplies;
import io.gunmetal.internal.ComponentTemplate;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author rees.byars
 */
public class MultiBindIntegrationTest {

    @Module(type = Module.Type.COMPONENT_PARAM)
    class NonEmptyModule {
        @Supplies @MultiBind Object object = "test";
    }

    @Module(type = Module.Type.COMPONENT)
    interface Component {
        @MultiBind List<Object> objects();
    }

    interface EmptyComponentFactory {
        Component create();
    }

    interface NonEmptyComponentFactory {
        Component create(NonEmptyModule nonEmptyModule);
    }

    @Test
    public void testEmpty() {
        assertEquals(0, ComponentTemplate.build(EmptyComponentFactory.class).create().objects().size());
    }

    @Test
    public void testNonEmpty() {
        assertEquals("test", ComponentTemplate.build(NonEmptyComponentFactory.class).create(new NonEmptyModule()).objects().get(0));
    }

}
