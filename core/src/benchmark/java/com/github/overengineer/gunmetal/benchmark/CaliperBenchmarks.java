package com.github.overengineer.gunmetal.benchmark;

import com.github.overengineer.gunmetal.Container;
import com.github.overengineer.gunmetal.Gunmetal;
import com.github.overengineer.gunmetal.SelectionAdvisor;
import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.key.Generic;
import com.github.overengineer.gunmetal.key.Qualifier;
import com.github.overengineer.gunmetal.key.Smithy;
import com.github.overengineer.gunmetal.testmocks.AA;
import com.github.overengineer.gunmetal.testmocks.E;
import com.github.overengineer.gunmetal.testmocks.N;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import dagger.ObjectGraph;
import se.jbee.inject.bootstrap.Bootstrap;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.Object;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author rees.byars
 */
public class CaliperBenchmarks {

    static final Dependency<N> PROTOTYPE_DEPENDENCY = Smithy.forge(N.class, Qualifier.NONE);
    static final Dependency<E> SINGLETON_DEPENDENCY  = Smithy.forge(E.class, Qualifier.NONE);
    Provider<N> gunmetalProvider;
    static final Container CONTAINER = Gunmetal.jsr330().load(new GunmetalBenchMarkModule());
    @Inject public Provider<N> daggerProvider;
    static final ObjectGraph OBJECT_GRAPH = ObjectGraph.create(new DaggerBenchMarkModule());
    static final Key<N> PROTOTYPE_KEY = Key.get(N.class);
    static final Key<E> SINGLETON_KEY = Key.get(E.class);
    Provider<N> guiceProvider;
    static final Injector INJECTOR = Guice.createInjector(new GuiceBenchMarkModule());

    @BeforeExperiment() void setUp() {
        gunmetalProvider = CONTAINER.get(new Generic<Provider<N>>() { });
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
        Dependency<N> dependency = PROTOTYPE_DEPENDENCY;
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
            dummy |= objectGraph.get(N.class).hashCode();
        }
        return dummy;
    }

    @Benchmark long guicePrototype(int reps) {
        Injector injector = INJECTOR;
        Key<N> key = PROTOTYPE_KEY;
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
        Provider<N> provider = gunmetalProvider;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= provider.get().hashCode();
        }
        return dummy;
    }

    @Benchmark long daggerProvider(int reps) {
        Provider<N> provider = daggerProvider;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= provider.get().hashCode();
        }
        return dummy;
    }

    @Benchmark long guiceProvider(int reps) {
        Provider<N> provider = guiceProvider;
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= provider.get().hashCode();
        }
        return dummy;
    }

    @Benchmark long testPure(int reps) {
        Instance instance = new Instance();
        HashMap map = new HashMap();
        map.put("key", instance);
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= ((Instance) map.get("key")).getComponent().execute();
        }
        return dummy;
    }

    @Benchmark long testInstanceProvider(int reps) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = Instance.class.getDeclaredMethod("getComponent");
        m.setAccessible(true);
        Instance instance = new Instance();
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= ((Component) m.invoke(instance)).execute();
        }
        return dummy;
    }

    @Benchmark long testConstructorProvider(int reps) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Constructor c = Component.class.getDeclaredConstructor();
        c.setAccessible(true);
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= ((Component) c.newInstance()).execute();
        }
        return dummy;
    }

    @Benchmark long testStaticProvider(int reps) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = Static.class.getDeclaredMethod("getComponent");
        m.setAccessible(true);
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= ((Component) m.invoke(null)).execute();
        }
        return dummy;
    }

    static class Instance {
        Component getComponent() {
            return new Component();
        }
    }

    static class Static {
        static Component getComponent() {
            return new Component();
        }
    }

    static class Component {
        static int i = 0;
        int execute() {
            return i++;
        }
    }

}
