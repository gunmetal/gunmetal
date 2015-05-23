package io.gunmetal.integration;

import io.gunmetal.Module;
import io.gunmetal.Supplies;
import io.gunmetal.internal.ComponentTemplate;
import io.gunmetal.sandbox.testmocks.D;
import io.gunmetal.sandbox.testmocks.E;
import io.gunmetal.sandbox.testmocks.SlimGunmetalBenchMarkModule;
import org.junit.Test;

import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author rees.byars
 */
public class SubComponentIntegrationTest {

    @Module(dependsOn = SlimGunmetalBenchMarkModule.class, type = Module.Type.COMPONENT)
    public interface SubComponent {

        @Supplies E e();

        @Supplies D d();

        public interface Factory {
            SubComponent create();
        }

    }

    @Module(type = Module.Type.COMPONENT)
    public interface RootComponent {

        void inject(Object o);

        Supplier<D> dSupplier();

        Supplier<E> eSupplier();

        public interface Factory {
            RootComponent create(SubComponent subComponent);
        }

    }

    @Test
    public void testSubComponent() {

        SubComponent.Factory subComponentFactory =
                ComponentTemplate.build(SubComponent.Factory.class);

        RootComponent.Factory rootComponentFactory =
                ComponentTemplate.build(RootComponent.Factory.class);

        SubComponent subComponent = subComponentFactory.create();
        RootComponent rootComponent = rootComponentFactory.create(subComponent);

        // TODO new component annotation to fix this

        assertTrue(rootComponent.eSupplier().get() == rootComponent.eSupplier().get());
        assertTrue(rootComponent.eSupplier().get() == subComponent.e());
        assertFalse(rootComponent.dSupplier().get() == rootComponent.dSupplier().get());

    }


}
