package com.github.overengineer.gunmetal.benchmark;

import com.github.overengineer.gunmetal.Container;
import com.github.overengineer.gunmetal.Gunmetal;
import com.github.overengineer.gunmetal.SelectionAdvisor;
import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.key.Qualifier;
import com.github.overengineer.gunmetal.key.Smithy;
import com.google.caliper.Benchmark;
import com.google.inject.Guice;
import dagger.ObjectGraph;

/**
 * @author rees.byars
 */
public class CaliperBenchmarks {

    static final Dependency<R> PROTOTYPE_DEPENDENCY = Smithy.forge(R.class, Qualifier.NONE);
    static final Dependency<E> SINGLETON_DEPENDENCY  = Smithy.forge(E.class, Qualifier.NONE);
    static final Container CONTAINER = Gunmetal.jsr330().load(new BenchMarkModule());
    static final ObjectGraph OBJECT_GRAPH = ObjectGraph.create(new DaggerBenchMarkModule());

    @Benchmark long gunmetalStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= Gunmetal.jsr330().load(new BenchMarkModule()).get(AA.class, SelectionAdvisor.NONE).hashCode();
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

    @Benchmark long gunmetalPrototype(int reps) {
        Container c = CONTAINER;
        Dependency<R> dependency = PROTOTYPE_DEPENDENCY;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= c.get(dependency, SelectionAdvisor.NONE).hashCode();
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

    @Benchmark long gunmetalSingleton(int reps) {
        Container c = CONTAINER;
        Dependency<E> dependency = SINGLETON_DEPENDENCY;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= c.get(dependency, SelectionAdvisor.NONE).hashCode();
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

}
