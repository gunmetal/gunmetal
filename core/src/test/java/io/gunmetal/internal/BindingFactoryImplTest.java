package io.gunmetal.internal;

import io.gunmetal.Inject;
import io.gunmetal.Module;
import io.gunmetal.spi.DependencySupplier;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BindingFactoryImplTest {

    @Inject BindingFactory bindingFactory;
    @Inject DependencySupplier dependencySupplier;
    @Inject ComponentContext componentContext;
    @Inject ComponentLinker componentLinker;
    @Inject ComponentRepository componentRepository;
    @Inject ComponentErrors componentErrors;

    @Module(dependsOn = BaseTestModule.class, type = Module.Type.COMPONENT)
    public interface TestComponent {

        void inject(BindingFactoryImplTest test);

        public interface Factory {

            TestComponent create();

        }

    }

    TestComponent graph = ComponentTemplate.build(TestComponent.Factory.class).create();

    @Module(type = Module.Type.CONSTRUCTED)
    static class EmptyStatefulModule {
    }

    @Before
    public void setUp() {
        graph.inject(this);
    }

    @Test
    public void testCreateBindingsForModule_empty() {

        @Module
        class MyModule {
        }

        List<Binding> bindings =
                bindingFactory.createBindingsForModule(MyModule.class, componentContext);

        assertEquals(0, bindings.size());

    }

    @Test
    public void testCreateBindingsForModule_emptyStatefulModule() {

        List<Binding> bindings =
                bindingFactory.createBindingsForModule(EmptyStatefulModule.class, componentContext);

        assertEquals(1, bindings.size());

        Binding binding = bindings.get(0);

        assertTrue(binding.resource().provisionStrategy().get(dependencySupplier, componentContext.newResolutionContext()) instanceof EmptyStatefulModule);

    }

    @Test
    public void testCreateJitBindingForRequest() {

    }

    @Test
    public void testCreateJitFactoryBindingsForRequest() {

    }

}