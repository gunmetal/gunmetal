package io.gunmetal.benchmarks;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.inject.Guice;
import com.google.inject.Key;
import dagger.ObjectGraph;
import io.gunmetal.Component;
import io.gunmetal.Module;
import io.gunmetal.internal.ComponentTemplate;
import io.gunmetal.sandbox.testmocks.AA;
import io.gunmetal.sandbox.testmocks.FieldGunmetalBenchMarkModule;
import io.gunmetal.sandbox.testmocks.N;
import io.gunmetal.sandbox.testmocks.SlimGunmetalBenchMarkModule;
import io.gunmetal.spi.GunmetalComponent;
import io.gunmetal.spi.Option;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import se.jbee.inject.bootstrap.Bootstrap;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.function.Supplier;

/**
 * mvn clean install -Pbenchmarks -DskipTests -Dbenchmark.instruments=runtime -Dbenchmark.args="--benchmark gunmetalStandup"
 *
 * @author rees.byars
 */
public class CaliperBenchmarks {

    SlimGunmetalBenchMarkModule.SlimComponent.Factory slimTemlplate;
    FieldGunmetalBenchMarkModule.Component.Factory fieldTemplate;
    Supplier<N> gunmetalProvider;
    Provider<N> guiceProvider;

    @BeforeExperiment
    void setUp() {

        slimTemlplate = Component.buildTemplate(SlimGunmetalBenchMarkModule.SlimComponent.Factory.class);

        fieldTemplate =
                Component.buildTemplate(FieldGunmetalBenchMarkModule.Component.Factory.class);

        gunmetalProvider = Component.buildTemplate(
                SlimGunmetalBenchMarkModule.SlimComponent.Factory.class)
                .create()
                .supplier();

        guiceProvider = Guice
                .createInjector(new GuiceBenchMarkModule())
                .getProvider(Key.get(N.class));

    }

    @Benchmark long gunmetalStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            InjectionTarget injectionTarget = new InjectionTarget();
            Component.buildTemplate(FieldGunmetalBenchMarkModule.Component.Factory.class)
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
            fieldTemplate.create().inject(injectionTarget);
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
            ComponentTemplate
                    .build(
                            new GunmetalComponent.Default(Option.REQUIRE_ACYCLIC),
                            SlimGunmetalBenchMarkModule.SlimComponent.Factory.class)
                    .create()
                    .inject(injectionTarget);
            dummy |= injectionTarget.hashCode();
        }
        return dummy;
    }

    @Benchmark long zeroGunmetalStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            InjectionTarget injectionTarget = new InjectionTarget();
            ComponentTemplate
                    .build(
                            new GunmetalComponent.Default(Option.REQUIRE_ACYCLIC),
                            ZeroComponent.Factory.class)
                    .create()
                    .inject(injectionTarget);
            dummy |= injectionTarget.hashCode();
        }
        return dummy;
    }

    @Benchmark long slimTemplate(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            InjectionTarget injectionTarget = new InjectionTarget();
            slimTemlplate.create().inject(injectionTarget);
            dummy |= injectionTarget.hashCode();
        }
        return dummy;
    }

    @Benchmark long slimDaggerStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            InjectionTarget injectionTarget = new InjectionTarget();
            ObjectGraph.create(new DaggerSlimBenchMarkModule()).inject(injectionTarget);
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

    @Benchmark long springStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= new AnnotationConfigApplicationContext(SpringBenchMarkModule.class).getBean(AA.class).hashCode();
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

    @Benchmark long guiceProvider(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= guiceProvider.get().hashCode();
        }
        return dummy;
    }

    @Benchmark long gunmetalProvider(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= gunmetalProvider.get().hashCode();
        }
        return dummy;
    }

    @Module(component = true)
    public interface ZeroComponent {

        void inject(Object o);

        public interface Factory {
            ZeroComponent create();
        }

    }

    static class InjectionTarget {

        @Inject
        AA aa;

        @io.gunmetal.Inject
        AA aaa;

    }

}
