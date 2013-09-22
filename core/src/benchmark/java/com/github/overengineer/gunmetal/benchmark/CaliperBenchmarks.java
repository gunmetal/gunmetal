package com.github.overengineer.gunmetal.benchmark;

import com.github.overengineer.gunmetal.Container;
import com.github.overengineer.gunmetal.Gunmetal;
import com.github.overengineer.gunmetal.SelectionAdvisor;
import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.key.Generic;
import com.github.overengineer.gunmetal.key.Qualifier;
import com.github.overengineer.gunmetal.key.Smithy;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import dagger.ObjectGraph;
import se.jbee.inject.bootstrap.Bootstrap;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author rees.byars
 */
public class CaliperBenchmarks {

    static final Dependency<R> PROTOTYPE_DEPENDENCY = Smithy.forge(R.class, Qualifier.NONE);
    static final Dependency<E> SINGLETON_DEPENDENCY  = Smithy.forge(E.class, Qualifier.NONE);
    Provider<R> gunmetalProvider;
    static final Container CONTAINER = Gunmetal.jsr330().load(new GunmetalBenchMarkModule());
    @Inject public Provider<R> daggerProvider;
    static final ObjectGraph OBJECT_GRAPH = ObjectGraph.create(new DaggerBenchMarkModule());
    static final Key<R> PROTOTYPE_KEY = Key.get(R.class);
    static final Key<E> SINGLETON_KEY = Key.get(E.class);
    Provider<R> guiceProvider;
    static final Injector INJECTOR = Guice.createInjector(new GuiceBenchMarkModule());

    @BeforeExperiment() void setUp() {
        gunmetalProvider = CONTAINER.get(new Generic<Provider<R>>() { });
        OBJECT_GRAPH.inject(this);
        guiceProvider = INJECTOR.getProvider(PROTOTYPE_KEY);
    }

    @Benchmark long gunmetalStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= Gunmetal.jsr330().load(new GunmetalBenchMarkModule()).get(AA.class, SelectionAdvisor.NONE).hashCode();
        }
        return dummy;
    }

    @Benchmark long daggerStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= ObjectGraph.create(new DaggerBenchMarkModule()).get(AA.class).hashCode();
        }
        return dummy;
    }

    @Benchmark long guiceStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= Guice.createInjector(new GuiceBenchMarkModule()).getInstance(AA.class).hashCode();
        }
        return dummy;
    }

    @Benchmark long silkStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= Bootstrap.injector(SilkBenchMarkModule.class).resolve(se.jbee.inject.Dependency.dependency(AA.class)).hashCode();
        }
        return dummy;
    }

    @Benchmark long picoStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= new PicoBenchMarkModule().get().getComponent(AA.class).hashCode();
        }
        return dummy;
    }

    @Benchmark long gunmetalPrototype(int reps) {
        Container container = CONTAINER;
        Dependency<R> dependency = PROTOTYPE_DEPENDENCY;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= container.get(dependency, SelectionAdvisor.NONE).hashCode();
        }
        return dummy;
    }

    @Benchmark long daggerPrototype(int reps) {
        ObjectGraph objectGraph = OBJECT_GRAPH;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= objectGraph.get(R.class).hashCode();
        }
        return dummy;
    }

    @Benchmark long guicePrototype(int reps) {
        Injector injector = INJECTOR;
        Key<R> key = PROTOTYPE_KEY;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= injector.getInstance(key).hashCode();
        }
        return dummy;
    }

    @Benchmark long gunmetalSingleton(int reps) {
        Container container = CONTAINER;
        Dependency<E> dependency = SINGLETON_DEPENDENCY;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= container.get(dependency, SelectionAdvisor.NONE).hashCode();
        }
        return dummy;
    }

    @Benchmark long daggerSingleton(int reps) {
        ObjectGraph objectGraph = OBJECT_GRAPH;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= objectGraph.get(E.class).hashCode();
        }
        return dummy;
    }

    @Benchmark long guiceSingleton(int reps) {
        Injector injector = INJECTOR;
        Key<E> key = SINGLETON_KEY;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= injector.getInstance(key).hashCode();
        }
        return dummy;
    }

    @Benchmark long gunmetalProvider(int reps) {
        Provider<R> provider = gunmetalProvider;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= provider.get().hashCode();
        }
        return dummy;
    }

    @Benchmark long daggerProvider(int reps) {
        Provider<R> provider = daggerProvider;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= provider.get().hashCode();
        }
        return dummy;
    }

    @Benchmark long guiceProvider(int reps) {
        Provider<R> provider = guiceProvider;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= provider.get().hashCode();
        }
        return dummy;
    }

}
