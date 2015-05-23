package io.gunmetal.integration;

import io.gunmetal.Inject;
import io.gunmetal.Module;
import io.gunmetal.internal.ComponentTemplate;
import io.gunmetal.sandbox.testmocks.D;
import io.gunmetal.sandbox.testmocks.E;
import io.gunmetal.sandbox.testmocks.SlimGunmetalBenchMarkModule;
import org.junit.Test;

import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * tests that scope decorations are consistent across different types of resource acquisition
 *
 * @author rees.byars
 */
public class ConsistentScopesIntegrationTest {

    @Module(dependsOn = SlimGunmetalBenchMarkModule.class, type = Module.Type.COMPONENT)
    public interface TestComponent {

        void inject(Object o);

        Supplier<D> dSupplier();

        Supplier<E> eSupplier();

        E e();

        D d();

    }

    public interface Factory {
        TestComponent create();
    }

    static class InjectTest {
        @Inject E e;
        @Inject D d;
        @Inject Supplier<E> eSupplier;
        @Inject Supplier<D> dSupplier;
    }

    @Test
    public void testConsistentScopes() {

        Factory factory = ComponentTemplate.build(Factory.class);

        TestComponent component1 = factory.create();
        InjectTest test1 = new InjectTest();

        component1.inject(test1);

        assertTrue(test1.e == component1.e());
        assertTrue(test1.d.e == component1.e());
        assertTrue(test1.e == component1.eSupplier().get());
        assertTrue(test1.eSupplier.get() == component1.eSupplier().get());

        assertFalse(test1.d == component1.d());
        assertFalse(test1.d == component1.dSupplier().get());
        assertFalse(test1.dSupplier.get() == component1.dSupplier().get());

        TestComponent component2 = factory.create();
        InjectTest test2 = new InjectTest();
        component2.inject(test2);

        assertTrue(test1.e == component1.e());
        assertTrue(test1.d.e == component1.e());
        assertTrue(test1.e == component1.eSupplier().get());
        assertTrue(test1.eSupplier.get() == component1.eSupplier().get());

        assertFalse(test1.e == test2.e);

    }
}
