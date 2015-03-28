package io.gunmetal.benchmarks;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import io.gunmetal.Module;
import io.gunmetal.sandbox.testmocks.AA;
import io.gunmetal.sandbox.testmocks.E;
import io.gunmetal.sandbox.testmocks.N;
import io.gunmetal.sandbox.testmocks.NewGunmetalBenchMarkModule;
import io.gunmetal.sandbox.testmocks.SlimGunmetalBenchMarkModule;
import se.jbee.inject.bootstrap.Bootstrap;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * mvn clean install -Pbenchmarks -DskipTests -Dbenchmark.instruments=runtime -Dbenchmark.args="--benchmark newGunmetalStandup,gunmetalStandup"
 *
 * @author rees.byars
 */
public class CaliperBenchmarks {

    @Inject Provider<N> newGunmetalProvider;
    @Inject public Provider<N> daggerProvider;
    dagger.ObjectGraph OBJECT_GRAPH;
    static final Key<N> PROTOTYPE_KEY = Key.get(N.class);
    static final Key<E> SINGLETON_KEY = Key.get(E.class);
    Provider<N> guiceProvider;
    Injector INJECTOR;
    FullComponent.Factory template;

    @Module(dependsOn = SlimGunmetalBenchMarkModule.class)
    public interface SlimComponent {

        void inject(Object o);

        public interface Factory {
            SlimComponent create();
        }

    }

    @Module(dependsOn = NewGunmetalBenchMarkModule.class)
    public interface FullComponent {

        void inject(Object o);

        public interface Factory {
            FullComponent create();
        }

    }

    @BeforeExperiment() void setUp() {
        OBJECT_GRAPH = dagger.ObjectGraph.create(new DaggerBenchMarkModule());
        INJECTOR = Guice.createInjector(new GuiceBenchMarkModule());
        OBJECT_GRAPH.inject(this);
        guiceProvider = INJECTOR.getProvider(PROTOTYPE_KEY);
        io.gunmetal.Component
                .builder()
                .requireAcyclic()
                .withJsr330Metadata()
                .build(SlimComponent.Factory.class)
                .create()
                .inject(this);
        template = io.gunmetal.Component.builder().build(FullComponent.Factory.class);

        guiceProvider(100);
        newGunmetalProvider(100);

    }

    @Benchmark long newGunmetalStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            InjectionTarget injectionTarget = new InjectionTarget();
            io.gunmetal.Component.builder()
                    .build(FullComponent.Factory.class)
                    .create()
                    .inject(injectionTarget);
            dummy |= injectionTarget.hashCode();
        }
        return dummy;
    }

    @Benchmark long template(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            InjectionTarget injectionTarget = new InjectionTarget();
            template.create().inject(injectionTarget);
            dummy |= injectionTarget.hashCode();
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

    @Benchmark long slimGunmetalStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            InjectionTarget injectionTarget = new InjectionTarget();
            io.gunmetal.Component.builder()
                    .requireAcyclic()
                    .build(SlimComponent.Factory.class)
                    .create()
                    .inject(injectionTarget);
            dummy |= injectionTarget.hashCode();
        }
        return dummy;
    }

    @Module
    public interface ZeroComponent {

        void inject(Object o);

        public interface Factory {
            ZeroComponent create();
        }

    }

    @Benchmark long zeroGunmetalStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            InjectionTarget injectionTarget = new InjectionTarget();
            io.gunmetal.Component.builder()
                    .requireAcyclic()
                    .build(ZeroComponent.Factory.class)
                    .create()
                    .inject(injectionTarget);
            dummy |= injectionTarget.hashCode();
        }
        return dummy;
    }

    @Benchmark long slimTemplate(int reps) {
        int dummy = 0;
        SlimComponent.Factory templateGraph = io.gunmetal.Component.builder()
                .requireAcyclic()
                .build(SlimComponent.Factory.class);
        for (long i = 0; i < reps; i++) {
            InjectionTarget injectionTarget = new InjectionTarget();
            templateGraph.create().inject(injectionTarget);
            dummy |= injectionTarget.hashCode();
        }
        return dummy;
    }

    @Benchmark long slimDaggerStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            InjectionTarget injectionTarget = new InjectionTarget();
            dagger.ObjectGraph.create(new DaggerSlimBenchMarkModule()).inject(injectionTarget);
            dummy |= injectionTarget.hashCode();
        }
        return dummy;
    }

    @Benchmark long zeroGuiceStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= Guice.createInjector().getInstance(AA.class).hashCode();
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
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= newGunmetalProvider.get().hashCode();
        }
        return dummy;
    }

    @Benchmark long gnewGunmetalProvider(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= newGunmetalProvider.get().hashCode();
        }
        return dummy;
    }

    @Benchmark long daggerProvider(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= daggerProvider.get().hashCode();
        }
        return dummy;
    }

    @Benchmark long guiceProvider(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= guiceProvider.get().hashCode();
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
