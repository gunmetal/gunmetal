/*
 * Copyright (c) 2013.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gunmetal.internal;

import io.gunmetal.MultiBind;
import io.gunmetal.FromModule;
import io.gunmetal.Inject;
import io.gunmetal.Lazy;
import io.gunmetal.Module;
import io.gunmetal.ObjectGraph;
import io.gunmetal.Overrides;
import io.gunmetal.Provider;
import io.gunmetal.Provides;
import io.gunmetal.Ref;
import io.gunmetal.Singleton;
import io.gunmetal.spi.ProvisionMetadata;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Scope;
import io.gunmetal.testmocks.A;
import io.gunmetal.testmocks.AA;
import io.gunmetal.testmocks.F;
import io.gunmetal.testmocks.N;
import io.gunmetal.testmocks.NewGunmetalBenchMarkModule;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author rees.byars
 */
public class ApplicationBuilderImplTest {

    @Retention(RetentionPolicy.RUNTIME)
    @io.gunmetal.Qualifier
    public @interface Main {}

    @Retention(RetentionPolicy.RUNTIME)
    @io.gunmetal.Qualifier
    public @interface Stateful {}

    @Retention(RetentionPolicy.RUNTIME)
    @io.gunmetal.Scope
    public @interface TestScope {}

    interface Bad { }

    static class Circ implements Bad {
        @Inject Bad providedCirc;
        @Inject ApplicationBuilderImplTest applicationBuilderImplTest;
    }

    @Module(notAccessibleFrom = TestModule.BlackList.class, dependsOn = StatefulModule.class)
    static class TestModule {

        @Provides @Singleton @Overrides(allowCycle = true) static Bad providedCirc(Provider<Circ> circProvider) {
            return circProvider.get();
        }

        @Provides @Overrides(allowCycle = true) static Circ circ() {
            return new Circ();
        }

        @io.gunmetal.BlackList.Modules(M.class)
        static class BlackList implements io.gunmetal.BlackList {

            @Inject
            TestModule testModule;


        }


        @Provides @Singleton static Class<Void> routine(BlackList blackList) {
            System.out.println("routineeee" + blackList);
            return Void.TYPE;
        }


        @Provides @Singleton @Overrides(allowMappingOverride = true) static TestModule tm(ArrayList<Integer> integers) {
            return new TestModule();
        }

        @Provides @Singleton static TestModule tmO() {
            return new TestModule();
        }

        @Provides @Singleton @Main static ApplicationBuilderImplTest test(ApplicationBuilderImplTest test) {
            return new ApplicationBuilderImplTest();
        }

        @Provides @Singleton static Object test2(Provider<ApplicationBuilderImplTest> test, BlackList blackList) {
            System.out.println(test.get());
            System.out.println(test.get());

            System.out.println(blackList);
            return test.get();
        }

        @Provides static ApplicationBuilderImplTest testy() {
            return new ApplicationBuilderImplTest();
        }

        @Provides @Singleton @MultiBind static String s1(@Stateful String name) {
            return name;
        }

        @Provides @Singleton @MultiBind static String s2() {
            return "2";
        }

        @Provides @Singleton @MultiBind static String s3() {
            return "3";
        }

        @Provides @Singleton @Main static InputStream printStrings(@MultiBind List<String> strings) {
            for (String s : strings) {
                System.out.println(s);
            }
            return System.in;
        }

        @Provides @Singleton @Lazy static List<? extends ProvisionStrategyDecorator> decorators() {
            return Collections.singletonList(new ProvisionStrategyDecorator() {
                @Override public <T> ProvisionStrategy<T> decorate(ProvisionMetadata<?> provisionMetadata, ProvisionStrategy<T> delegateStrategy, Linkers linkers) {
                    return delegateStrategy;
                }
            });
        }

        @Provides @Singleton @Lazy static Map<? extends Scope, ? extends ProvisionStrategyDecorator> scopeDecorators() {
            return Collections.singletonMap(
                    CustomScopes.TEST,
                    new ProvisionStrategyDecorator() {
                        @Override public <T> ProvisionStrategy<T> decorate(ProvisionMetadata<?> provisionMetadata, ProvisionStrategy<T> delegateStrategy, Linkers linkers) {
                            return delegateStrategy;
                        }
                    });
        }

    }

    @Module(stateful = true)
    @Stateful
    @Singleton
    static class StatefulModule {

        String name;

        StatefulModule(String name) {
            this.name = name;
        }

        @Provides @Singleton String name(ApplicationBuilderImplTest test) {
            return name + test.getClass().getName();
        }

        @Provides @Singleton List<StatefulModule> statefulModules(@FromModule Ref<StatefulModule> statefulModuleRef) {
            assert statefulModuleRef.get() == statefulModuleRef.get();
            return Collections.singletonList(statefulModuleRef.get());
        }

    }

    @Module
    static class M {
        @Provides @Singleton @Lazy static M m(ApplicationBuilderImplTest test) {
            return new M();
        }
    }

    enum CustomScopes implements Scope {
        TEST;
        @Override public boolean canInject(Scope scope) {
            return scope.equals(scope);
        }
    }

    @Test
    public void testBuild() {

        ObjectGraph app = ObjectGraph.builder()
                .requireAcyclic()
                .buildTemplate(TestModule.class)
                .newInstance(new StatefulModule("rees"));

        app = app.newInstance(new StatefulModule("rees"));

        @Main class Dep implements io.gunmetal.Dependency<ApplicationBuilderImplTest> { }


        ApplicationBuilderImplTest test = app.get(Dep.class);


        class BadDep implements io.gunmetal.Dependency<Circ> { }

        Bad b = app.get(BadDep.class);

        Bad b2 = app.get(BadDep.class);

        Bad b3 = app.get(BadDep.class);

        assert b != app.get(BadDep.class);

        assert ((Circ) b).providedCirc != app.get(BadDep.class);

        assert test != this;

        class Dep2 implements io.gunmetal.Dependency<A> { }

        app = ObjectGraph
                .builder()
                .buildTemplate(NewGunmetalBenchMarkModule.class)
                .newInstance();

        A a = app.get(Dep2.class);

        assert a != app.get(Dep2.class);

        class InjectTest {
            @Inject
            F f;
        }

        class InjectTest2 {
            @Inject
            F f;
        }

        InjectTest injectTest = new InjectTest();

        app.inject(injectTest);

        app.inject(new InjectTest2());

        assert injectTest.f != null;

    }

    @Test(expected = RuntimeException.class)
    public void testBlackList() {
        new GraphBuilder().buildTemplate(TestModule.class, M.class);
    }

    @Module(subsumes = MyLibrary.class)
    @Main
    static class PlusModule implements Cheese {

        @Inject ApplicationBuilderImplTest applicationBuilderImplTest;

        @Provides @Singleton static PlusModule plusModule() {
            return new PlusModule();
        }

        @Provides @Singleton static Cheese cheese() {
            return new PlusModule();
        }

        @Provides @Singleton static OutputStream r(@FromModule Cheese cheese, @Main Lib myLibrary) {
            System.out.println("sup");
            return System.out;
        }

    }

    interface Lib { }

    @Module(lib = true)
    static class MyLibrary implements Lib {

        @Provides @Singleton static Lib huh(@FromModule Cheese cheese) {
            System.out.println("sup library");
            return new MyLibrary();
        }

    }

    interface Cheese { }

    @Test
    public void testPlus() {

        @Main
        class Dep implements io.gunmetal.Dependency<PlusModule> { }

        ObjectGraph parent = ObjectGraph.builder()
                .buildTemplate(TestModule.class)
                .newInstance(new StatefulModule("plus"));

        ObjectGraph child = parent.plus().buildTemplate(PlusModule.class).newInstance();

        PlusModule p = child.get(Dep.class);

        assert p.applicationBuilderImplTest != null;

        assert parent.get(Dep.class) == null;

        class InjectTest {
            @Inject
            F f;
        }

        class InjectTest2 {
            @Inject
            F f;
        }

        InjectTest injectTest = new InjectTest();

        child.inject(injectTest);

        child.inject(new InjectTest2());

        assert injectTest.f != null;

        ObjectGraph childCopy = child.newInstance();

        assert child.get(Dep.class) != childCopy.get(Dep.class);

        assert childCopy.get(Dep.class) == childCopy.get(Dep.class);

        assert childCopy.get(Dep.class) != null;

        childCopy.inject(injectTest);

    }

    @Test
    public void testMore() {
        class ProviderDep implements io.gunmetal.Dependency<io.gunmetal.Provider<N>> { }
        newGunmetalProvider = APPLICATION_CONTAINER.get(ProviderDep.class);
        newGunmetalStandup(10000);
    }

    io.gunmetal.Provider<N> newGunmetalProvider;
    static final ObjectGraph APPLICATION_CONTAINER = ObjectGraph.builder().buildTemplate(NewGunmetalBenchMarkModule.class).newInstance();

    static class Dep implements io.gunmetal.Dependency<AA> { }

    long newGunmetalStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= newGunmetalProvider.get().hashCode();
        }
        return dummy;
    }

}
