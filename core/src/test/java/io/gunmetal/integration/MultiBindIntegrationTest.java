package io.gunmetal.integration;

import io.gunmetal.Module;
import io.gunmetal.MultiBind;
import io.gunmetal.Supplies;
import io.gunmetal.internal.ComponentTemplate;
import org.junit.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

/**
 * @author rees.byars
 */
public class MultiBindIntegrationTest {

    @Module
    class NonEmptyModule {
        @Supplies @MultiBind Object object = "test";
    }

    @Module(component = true)
    interface Component {

        @MultiBind Supplier<List<Object>> objectsSupplier();

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
    public void testEmptyProvider() {
        assertEquals(0, ComponentTemplate.build(EmptyComponentFactory.class).create().objectsSupplier().get().size());
    }

    @Test
    public void testNonEmpty() {
        assertEquals("test", ComponentTemplate.build(NonEmptyComponentFactory.class).create(new NonEmptyModule()).objects().get(0));
    }

    @Test
    public void testNonEmptyProvider() {
        assertEquals("test", ComponentTemplate.build(NonEmptyComponentFactory.class).create(new NonEmptyModule()).objectsSupplier().get().get(0));
    }

}
