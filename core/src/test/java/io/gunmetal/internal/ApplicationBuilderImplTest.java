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

import io.gunmetal.AutoCollection;
import io.gunmetal.FromModule;
import io.gunmetal.Inject;
import io.gunmetal.Lazy;
import io.gunmetal.Library;
import io.gunmetal.Module;
import io.gunmetal.ObjectGraph;
import io.gunmetal.OverrideEnabled;
import io.gunmetal.Prototype;
import io.gunmetal.Provider;
import io.gunmetal.Ref;
import io.gunmetal.spi.ComponentMetadata;
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
    @io.gunmetal.Scope(scopeEnum = CustomScopes.class, name = "TEST")
    public @interface TestScope {}

    interface Bad { }

    static class Circ implements Bad {
        @Inject Bad providedCirc;
        @Inject ApplicationBuilderImplTest applicationBuilderImplTest;
    }

    @Module(notAccessibleFrom = TestModule.BlackList.class, dependsOn = StatefulModule.class)
    static class TestModule {

        static Bad providedCirc(Provider<Circ> circProvider) {
            return circProvider.get();
        }

        @Prototype static Circ circ() {
            return new Circ();
        }

        @io.gunmetal.BlackList.Modules(M.class)
        static class BlackList implements io.gunmetal.BlackList {

            @Inject
            TestModule testModule;


        }


        static Class<Void> routine(BlackList blackList) {
            System.out.println("routineeee" + blackList);
            return Void.TYPE;
        }


        @OverrideEnabled static TestModule tm(ArrayList<Integer> integers) {
            return new TestModule();
        }

        static TestModule tmO() {
            return new TestModule();
        }

        @Main static ApplicationBuilderImplTest test(ApplicationBuilderImplTest test) {
            return new ApplicationBuilderImplTest();
        }

        static Object test2(Provider<ApplicationBuilderImplTest> test, BlackList blackList) {
            System.out.println(test.get());
            System.out.println(test.get());

            System.out.println(blackList);
            return test.get();
        }

        @Prototype static ApplicationBuilderImplTest testy() {
            return new ApplicationBuilderImplTest();
        }

        @AutoCollection static String s1(@Stateful String name) {
            return name;
        }

        @AutoCollection static String s2() {
            return "2";
        }

        @AutoCollection static String s3() {
            return "3";
        }

        @Main static InputStream printStrings(@AutoCollection List<String> strings) {
            for (String s : strings) {
                System.out.println(s);
            }
            return System.in;
        }

        @Lazy static List<? extends ProvisionStrategyDecorator> decorators() {
            return Collections.singletonList(new ProvisionStrategyDecorator() {
                @Override public <T> ProvisionStrategy<T> decorate(ComponentMetadata<?> componentMetadata, ProvisionStrategy<T> delegateStrategy, Linkers linkers) {
                    return delegateStrategy;
                }
            });
        }

        @Lazy static Map<? extends Scope, ? extends ProvisionStrategyDecorator> scopeDecorators() {
            return Collections.singletonMap(
                    CustomScopes.TEST,
                    new ProvisionStrategyDecorator() {
                        @Override public <T> ProvisionStrategy<T> decorate(ComponentMetadata<?> componentMetadata, ProvisionStrategy<T> delegateStrategy, Linkers linkers) {
                            return delegateStrategy;
                        }
                    });
        }

    }

    @Module(stateful = true)
    @Stateful
    static class StatefulModule {

        String name;

        StatefulModule(String name) {
            this.name = name;
        }

        String name(ApplicationBuilderImplTest test) {
            return name + test.getClass().getName();
        }

        @Lazy StatefulModule statefulModule() {
            return this;
        }

        List<StatefulModule> statefulModules(@FromModule Ref<StatefulModule> statefulModuleRef) {
            assert statefulModuleRef.get() == statefulModuleRef.get();
            return Collections.singletonList(statefulModuleRef.get());
        }

    }

    @Module
    static class M {
        @Lazy static M m(ApplicationBuilderImplTest test) {
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

        InjectTest injectTest = new InjectTest();

        app.inject(injectTest);

        assert injectTest.f != null;

    }

    @Test(expected = DependencyException.class)
    public void testBlackList() {
        new GraphBuilder().buildTemplate(TestModule.class, M.class);
    }

    @Module(subsumes = MyLibrary.class)
    @Main
    static class PlusModule implements Cheese {

        @Inject ApplicationBuilderImplTest applicationBuilderImplTest;

        static PlusModule plusModule() {
            return new PlusModule();
        }

        static Cheese cheese() {
            return new PlusModule();
        }

        static OutputStream r(@FromModule Cheese cheese, @Main Lib myLibrary) {
            System.out.println("sup");
            return System.out;
        }

    }

    interface Lib { }

    @Library
    static class MyLibrary implements Lib {

        static Lib huh(@FromModule Cheese cheese) {
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

        InjectTest injectTest = new InjectTest();

        child.inject(injectTest);

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
