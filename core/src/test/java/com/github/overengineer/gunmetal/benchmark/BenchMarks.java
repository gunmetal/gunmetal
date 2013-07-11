package com.github.overengineer.gunmetal.benchmark;

import com.github.overengineer.gunmetal.Container;
import com.github.overengineer.gunmetal.Gunmetal;
import com.github.overengineer.gunmetal.SelectionAdvisor;
import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.key.Generic;
import com.github.overengineer.gunmetal.key.Smithy;
import com.github.overengineer.gunmetal.key.Qualifier;
import com.github.overengineer.gunmetal.module.BaseModule;
import com.github.overengineer.gunmetal.testutil.ConcurrentExecutionAssistant;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import dagger.ObjectGraph;
import se.jbee.inject.bootstrap.Bootstrap;

import org.junit.Test;

import javax.inject.Provider;
import java.lang.reflect.TypeVariable;


/**
 */
public class BenchMarks {

    @Test
    public void testContainerCreationSpeed() throws Exception {

        int threads = 4;
        long duration = 5000;
        long primingRuns = 100000;

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                Gunmetal.jsr330().load(new BenchMarkModule()).get(AA.class, SelectionAdvisor.NONE);
            }
        }, threads).run(duration, primingRuns, "my container creation");

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                Gunmetal.jsr330().load(new BaseModule() {
                    @Override
                    public void configure() { }
                }).get(AA.class, SelectionAdvisor.NONE);
            }
        }, threads).run(duration, primingRuns, "my container creation");

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                ObjectGraph.create(new DaggerSlimBenchMarkModule()).get(AA.class);
            }
        }, threads).run(duration, primingRuns, "dagger slim container creation");

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                ObjectGraph.create(new DaggerBenchMarkModule()).get(AA.class);
            }
        }, threads).run(duration, primingRuns, "dagger container creation");

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                Bootstrap.injector(SilkBenchMarkModule.class).resolve(se.jbee.inject.Dependency.dependency(AA.class));
            }
        }, threads).run(duration, primingRuns, "silk container creation");

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                new PicoBenchMarkModule().get().getComponent(AA.class);
            }
        }, threads).run(duration, primingRuns, "pico container creation");

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                Guice.createInjector(new GuiceBenchMarkModule()).getInstance(AA.class);
            }
        }, threads).run(duration, primingRuns, "guice container creation");

    }

    @Test
    public void testPrototypeSpeed() throws Exception {

        int threads = 20;
        long duration = 5000;
        long primingRuns = 2000000;


        final Dependency dependency = Smithy.forge(R.class);

        final Container container = Gunmetal.jsr330().load(new BenchMarkModule());

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                container.get(dependency, SelectionAdvisor.NONE);
            }
        }, threads).run(duration, primingRuns, "my prototype creation");



        final Injector injector = Guice.createInjector(new GuiceBenchMarkModule());

        final Key k = Key.get(R.class);

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                injector.getInstance(k);
            }
        }, threads).run(duration, primingRuns, "guice prototype creation");



        final ObjectGraph objectGraph = ObjectGraph.create(new DaggerBenchMarkModule());

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                objectGraph.get(R.class);
            }
        }, threads).run(duration, primingRuns, "dagger prototype creation");



    }


    @Test
    public void testProviderSpeed() throws Exception {

        int threads = 20;
        long duration = 5000;
        long primingRuns = 2000000;


        final Dependency<Provider<R>> dependency = new Generic<Provider<R>>() {};

        final Container container = Gunmetal.jsr330().load(new BenchMarkModule());

        final Provider<R> myProvider = container.get(dependency, SelectionAdvisor.NONE);

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                myProvider.get();
            }
        }, threads).run(duration, primingRuns, "my provider gets");



        final Injector injector = Guice.createInjector(new GuiceBenchMarkModule());

        final Key<Provider<R>> k = Key.get(new TypeLiteral<Provider<R>>() {});

        final Provider<R> guiceProvider = injector.getInstance(k);

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                guiceProvider.get();
            }
        }, threads).run(duration, primingRuns, "guice provider gets");

    }


    @Test
    public void testSingletonSpeed() throws Exception {

        int threads = 20;
        long duration = 5000;
        long primingRuns = 10000000;

        final Dependency<E> key = Smithy.forge(E.class, Qualifier.NONE);

        final Container container = Gunmetal.jsr330().load(new BenchMarkModule());

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                container.get(key, SelectionAdvisor.NONE);
            }
        }, threads).run(duration, primingRuns, "my singleton");

        final ObjectGraph objectGraph = ObjectGraph.create(new DaggerBenchMarkModule());

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                objectGraph.get(E.class);
            }
        }, threads).run(duration, primingRuns, "dagger singleton");

        final Injector injector = Guice.createInjector(new GuiceBenchMarkModule());

        final Key k = Key.get(E.class);

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                injector.getInstance(k);
            }
        }, threads).run(duration, primingRuns, "guice singleton");

    }

}
