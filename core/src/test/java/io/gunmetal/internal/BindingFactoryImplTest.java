package io.gunmetal.internal;

import io.gunmetal.Inject;
import io.gunmetal.Module;
import io.gunmetal.ObjectGraph;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ResolutionContext;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BindingFactoryImplTest {

    @Inject BindingFactory bindingFactory;
    @Inject InternalProvider internalProvider;
    @Inject GraphContext graphContext;
    @Inject GraphLinker graphLinker;
    @Inject GraphCache graphCache;
    @Inject GraphErrors graphErrors;

    ObjectGraph graph = ObjectGraph.builder().buildTemplate(BaseTestModule.class).newInstance();

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
                bindingFactory.createBindingsForModule(MyModule.class, graphContext, new HashSet<>());

        assertEquals(0, bindings.size());

    }

    @Test
    public void testCreateBindingsForModule_emptyStatefulModule() {

        List<Binding> bindings =
                bindingFactory.createBindingsForModule(EmptyStatefulModule.class, graphContext, new HashSet<>());

        assertEquals(1, bindings.size());

        Binding binding = bindings.get(0);

        assertTrue(binding.resource().provisionStrategy().get(internalProvider, ResolutionContext.create()) instanceof EmptyStatefulModule);

    }

    @Test
    public void testCreateJitBindingForRequest() {

    }

    @Test
    public void testCreateJitFactoryBindingsForRequest() {

    }

}