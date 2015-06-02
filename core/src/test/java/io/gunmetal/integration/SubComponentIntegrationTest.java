package io.gunmetal.integration;

import io.gunmetal.Component;
import io.gunmetal.Module;
import io.gunmetal.Supplies;
import io.gunmetal.sandbox.testmocks.D;
import io.gunmetal.sandbox.testmocks.E;
import io.gunmetal.sandbox.testmocks.SlimGunmetalBenchMarkModule;
import org.junit.Test;

import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author rees.byars
 */
public class SubComponentIntegrationTest {

    class Thing {
        String name;
    }

    @Module(dependsOn = ThingModule.class)
    class ProvidedModule {

        private String name;

        ProvidedModule(String name) {
            this.name = name;
        }

        @Supplies String object(Thing thing) {
            return name + thing.name;
        }

    }

    @Module
    class ThingModule {
        @Supplies Thing thing() {
            Thing thing = new Thing();
            thing.name = "_fromThingModule";
            return thing;
        }
    }

    @Module(dependsOn = SlimGunmetalBenchMarkModule.class, component = true)
    public interface SubComponent {

        @Supplies E e();

        @Supplies D d();

        public interface Factory {
            SubComponent create();
        }

    }

    @Module(component = true)
    public interface RootComponent {

        void inject(Object o);

        Supplier<D> dSupplier();

        Supplier<E> eSupplier();

        String string();

        public interface Factory {
            RootComponent create(SubComponent subComponent, ProvidedModule providedModule);
        }

    }

    @Test
    public void testSubComponent() {

        SubComponent.Factory subComponentFactory =
                Component.buildTemplate(SubComponent.Factory.class);

        RootComponent.Factory rootComponentFactory =
                Component.buildTemplate(RootComponent.Factory.class);

        SubComponent subComponent = subComponentFactory.create();
        RootComponent rootComponent = rootComponentFactory.create(
                subComponent,
                new ProvidedModule("test"));

        // TODO new component annotation to fix this

        assertTrue(rootComponent.eSupplier().get() == rootComponent.eSupplier().get());
        assertTrue(rootComponent.eSupplier().get() == subComponent.e());
        assertFalse(rootComponent.dSupplier().get() == rootComponent.dSupplier().get());

        assertEquals("test_fromThingModule", rootComponent.string());

    }


}
