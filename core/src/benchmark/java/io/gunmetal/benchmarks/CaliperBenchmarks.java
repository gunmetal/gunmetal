package io.gunmetal.benchmarks;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import io.gunmetal.ObjectGraph;
import io.gunmetal.TemplateGraph;
import io.gunmetal.testmocks.AA;
import io.gunmetal.testmocks.E;
import io.gunmetal.testmocks.N;
import io.gunmetal.testmocks.NewGunmetalBenchMarkModule;
import se.jbee.inject.bootstrap.Bootstrap;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 *
 * mvn clean install -Pbenchmarks -DskipTests -Dbenchmark.instruments=runtime -Dbenchmark.args="--benchmark newGunmetalStandup,gunmetalStandup"
 *
 * @author rees.byars
 */
public class CaliperBenchmarks {

    io.gunmetal.Provider<N> newGunmetalProvider;
    ObjectGraph APPLICATION_CONTAINER;
    @Inject public Provider<N> daggerProvider;
    dagger.ObjectGraph OBJECT_GRAPH;
    static final Key<N> PROTOTYPE_KEY = Key.get(N.class);
    static final Key<E> SINGLETON_KEY = Key.get(E.class);
    Provider<N> guiceProvider;
    Injector INJECTOR;
    TemplateGraph template;

    static class Dep implements io.gunmetal.Dependency<AA> { }

    @BeforeExperiment() void setUp() {
        OBJECT_GRAPH = dagger.ObjectGraph.create(new DaggerBenchMarkModule());
        INJECTOR = Guice.createInjector(new GuiceBenchMarkModule());
        APPLICATION_CONTAINER = io.gunmetal.ObjectGraph.builder().build(NewGunmetalBenchMarkModule.class).newInstance();
        OBJECT_GRAPH.inject(this);
        guiceProvider = INJECTOR.getProvider(PROTOTYPE_KEY);
        class ProviderDep implements io.gunmetal.Dependency<io.gunmetal.Provider<N>> { }
        newGunmetalProvider = APPLICATION_CONTAINER.get(ProviderDep.class);
        template = io.gunmetal.ObjectGraph.builder().build(NewGunmetalBenchMarkModule.class);
    }

    @Benchmark long newGunmetalStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            InjectionTarget injectionTarget = new InjectionTarget();
            io.gunmetal.ObjectGraph.builder().build(NewGunmetalBenchMarkModule.class).newInstance().inject(injectionTarget);
            dummy |= injectionTarget.hashCode();
        }
        return dummy;
    }

    @Benchmark long template(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= template.newInstance().get(Dep.class).hashCode();
        }
        return dummy;
    }

    @Benchmark long daggerStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            InjectionTarget injectionTarget = new InjectionTarget();
            dagger.ObjectGraph.create(new DaggerBenchMarkModule()).inject(injectionTarget);
            dummy |= injectionTarget.hashCode();
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

    @Benchmark long daggerPrototype(int reps) {
        dagger.ObjectGraph objectGraph = OBJECT_GRAPH;
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

    @Benchmark long daggerSingleton(int reps) {
        dagger.ObjectGraph objectGraph = OBJECT_GRAPH;
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

    @Benchmark long newGunmetalProvider(int reps) {
        io.gunmetal.Provider<N> provider = newGunmetalProvider;
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
            dummy |= ((Instance) map.get("key")).getComponent().hashCode();
        }
        return dummy;
    }

    @Benchmark long testInstanceProvider(int reps) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = Instance.class.getDeclaredMethod("getComponent");
        m.setAccessible(true);
        Instance instance = new Instance();
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= ((Component) m.invoke(instance)).hashCode();
        }
        return dummy;
    }

    @Benchmark long testConstructorProvider(int reps) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Constructor c = Component.class.getDeclaredConstructor();
        c.setAccessible(true);
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= ((Component) c.newInstance()).hashCode();
        }
        return dummy;
    }

    @Benchmark long testStaticProvider(int reps) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = Static.class.getDeclaredMethod("getComponent");
        m.setAccessible(true);
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= ((Component) m.invoke(null)).hashCode();
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

    static class InjectionTarget {

        @Inject
        AA aa;

        @io.gunmetal.Inject
        AA aaa;

    }


}
