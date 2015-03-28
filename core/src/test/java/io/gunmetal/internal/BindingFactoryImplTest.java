package io.gunmetal.internal;

import io.gunmetal.Inject;
import io.gunmetal.Module;
import io.gunmetal.Component;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.ResolutionContext;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
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

    @Module(dependsOn = BaseTestModule.class)
    public interface TestComponent {

        void inject(BindingFactoryImplTest test);

        public interface Factory {

            TestComponent create();

        }

    }

    TestComponent graph = Component
            .builder()
            .build(TestComponent.Factory.class)
            .create();

    @Module(provided = false, stateful = true)
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
                bindingFactory.createBindingsForModule(MyModule.class, componentContext, new HashSet<>());

        assertEquals(0, bindings.size());

    }

    @Test
    public void testCreateBindingsForModule_emptyStatefulModule() {

        List<Binding> bindings =
                bindingFactory.createBindingsForModule(EmptyStatefulModule.class, componentContext, new HashSet<>());

        assertEquals(1, bindings.size());

        Binding binding = bindings.get(0);

        assertTrue(binding.resource().provisionStrategy().get(dependencySupplier, ResolutionContext.create()) instanceof EmptyStatefulModule);

    }

    @Test
    public void testCreateJitBindingForRequest() {

    }

    @Test
    public void testCreateJitFactoryBindingsForRequest() {

    }

}