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

package io.gunmetal.sandbox;

import io.gunmetal.Factory;
import io.gunmetal.FromModule;
import io.gunmetal.Inject;
import io.gunmetal.Lazy;
import io.gunmetal.Module;
import io.gunmetal.MultiBind;
import io.gunmetal.ObjectGraph;
import io.gunmetal.Overrides;
import io.gunmetal.Param;
import io.gunmetal.Provider;
import io.gunmetal.Provides;
import io.gunmetal.Ref;
import io.gunmetal.Singleton;
import io.gunmetal.TemplateGraph;
import io.gunmetal.internal.GraphBuilder;
import io.gunmetal.sandbox.testmocks.A;
import io.gunmetal.sandbox.testmocks.AA;
import io.gunmetal.sandbox.testmocks.F;
import io.gunmetal.sandbox.testmocks.N;
import io.gunmetal.sandbox.testmocks.NewGunmetalBenchMarkModule;
import io.gunmetal.spi.Converter;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Scope;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author rees.byars
 */
public class SandboxIntegrationTest {

    @Retention(RetentionPolicy.RUNTIME)
    @io.gunmetal.Qualifier
    public @interface Main {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @io.gunmetal.Qualifier
    public @interface Stateful {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @io.gunmetal.Scope
    public @interface TestScope {
    }

    interface Bad {
    }

    static class Circ implements Bad {
        @Inject Bad providedCirc;
        @Inject SandboxIntegrationTest sandboxIntegrationTest;
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

        @Provides @Singleton static TestModule tmO(@Param String name) {
            System.out.println(name);
            return new TestModule();
        }

        @Provides @Singleton @Main static SandboxIntegrationTest test(SandboxIntegrationTest test) {
            return new SandboxIntegrationTest();
        }

        @Provides @Singleton static Object test2(Provider<SandboxIntegrationTest> test, BlackList blackList) {
            System.out.println(test.get());
            System.out.println(test.get());

            System.out.println(blackList);
            return test.get();
        }

        @Provides static SandboxIntegrationTest testy() {
            return new SandboxIntegrationTest();
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
            return Collections.singletonList((resourceMetadata, delegateStrategy, linkers) -> delegateStrategy);
        }

        @Provides @Singleton @Lazy static Map<? extends Scope, ? extends ProvisionStrategyDecorator> scopeDecorators() {
            return Collections.singletonMap(
                    CustomScopes.TEST,
                    (resourceMetadata, delegateStrategy, linkers) -> delegateStrategy);
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

        @Provides @Singleton String name(SandboxIntegrationTest test) {
            return name + test.getClass().getName();
        }

        @Provides @Singleton List<StatefulModule> statefulModules(@FromModule Ref<StatefulModule> statefulModuleRef) {
            assert statefulModuleRef.get() == statefulModuleRef.get();
            return Collections.singletonList(statefulModuleRef.get());
        }

    }

    @Module
    static class M {
        @Provides @Singleton @Lazy static M m(SandboxIntegrationTest test) {
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

        TemplateGraph templateGraph = ObjectGraph.builder()
                .requireAcyclic()
                .buildTemplate(TestModule.class);
        templateGraph.newInstance(new StatefulModule("rees"));

        ObjectGraph app = templateGraph.newInstance(new StatefulModule("rees"));

        class Dep {
            @Inject @Main SandboxIntegrationTest test;
        }


        Dep dep = new Dep();
        app.inject(dep);
        SandboxIntegrationTest test = dep.test;


        class BadDep {
            @Inject Circ circ;
        }

        BadDep badDep = new BadDep();
        BadDep badDep2 = new BadDep();
        BadDep badDep3 = new BadDep();
        BadDep badDep4 = new BadDep();
        app.inject(badDep).inject(badDep2).inject(badDep3).inject(badDep4);

        assert badDep.circ != badDep4.circ;

        BadDep badDep5 = new BadDep();
        app.inject(badDep5);
        assert (badDep.circ).providedCirc != badDep5.circ;

        assert test != this;

        class Dep2 {
            @Inject A a;
        }

        Dep2 dep2 = new Dep2();

        app = ObjectGraph
                .builder()
                .buildTemplate(NewGunmetalBenchMarkModule.class)
                .newInstance();

        app.inject(dep2);
        A a = dep2.a;

        app.inject(dep2);
        assert a != dep2.a;

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

        @Inject SandboxIntegrationTest sandboxIntegrationTest;

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

    interface Lib {
    }

    @Module(lib = true)
    static class MyLibrary implements Lib {

        @Provides @Singleton static Lib huh(@FromModule Cheese cheese) {
            System.out.println("sup library");
            return new MyLibrary();
        }

    }

    interface Cheese {
    }

    @Test
    public void testPlus() {


        class Dep {
            @Inject @Main PlusModule plusModule;
        }

        Dep dep = new Dep();

        ObjectGraph parent = ObjectGraph.builder()
                .buildTemplate(TestModule.class)
                .newInstance(new StatefulModule("plus"));

        TemplateGraph childTemplate = parent.plus().buildTemplate(PlusModule.class);
        ObjectGraph child = childTemplate.newInstance();

        child.inject(dep);
        PlusModule p = dep.plusModule;

        assert p.sandboxIntegrationTest != null;

        dep.plusModule = null;
        parent.inject(dep);
        assert dep.plusModule != null;

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

        ObjectGraph childCopy = childTemplate.newInstance();
        Dep dep2 = new Dep();
        child.inject(dep);
        childCopy.inject(dep2);
        assert dep.plusModule != dep2.plusModule;

        child.inject(dep2);
        assert dep.plusModule == dep2.plusModule;

        childCopy.inject(injectTest);

    }

    @Test
    public void testMore() {
        class ProviderDep {
            @Inject Provider<N> nProvider;
        }
        ProviderDep p =  new ProviderDep();
        APPLICATION_CONTAINER.inject(p);
        newGunmetalProvider = p.nProvider;
        newGunmetalStandup(10000);
    }

    io.gunmetal.Provider<N> newGunmetalProvider;
    static final ObjectGraph APPLICATION_CONTAINER = ObjectGraph.builder().buildTemplate(NewGunmetalBenchMarkModule.class).newInstance();

    static class AaHolder {
        @Inject AA aa;
    }

    long newGunmetalStandup(int reps) {
        int dummy = 0;
        for (long i = 0; i < reps; i++) {
            dummy |= newGunmetalProvider.get().hashCode();
        }
        return dummy;
    }

    @Module static class ConversionModule {

        @Inject Long numberLong;

        @Provides static String numberString() {
            return "3425";
        }

    }


    @Test
    public void testConversion() {

        ObjectGraph graph =
                ObjectGraph.builder()
                        .withConverterProvider(to -> {
                            if (to.raw().equals(Long.class) || to.raw().equals(long.class)) {
                                return Collections.singletonList(new Converter() {
                                    @Override public List<Class<?>> supportedFromTypes() {
                                        return Arrays.asList(String.class);
                                    }
                                    @Override public Object convert(Object from) {
                                        return Long.valueOf(from.toString());
                                    }
                                });
                            }
                            return Collections.emptyList();
                        }).buildTemplate(ConversionModule.class).newInstance();

        ConversionModule c = new ConversionModule();
        graph.inject(c);

        assertEquals(3425L, (long) c.numberLong);
    }

    @Module
    static class MyModule {
        String name;
        @Provides static MyModule myModule(@Param String name) {
            MyModule m = new MyModule();
            m.name = name;
            return m;
        }
    }

    @Factory
    static interface MyComponent {
        MyModule getMyModule(@Param String name);
    }

    @Test
    public void testComponent() {
        MyComponent component = ObjectGraph
                .builder()
                .buildTemplate(MyModule.class)
                .newInstance()
                .create(MyComponent.class);
        assertEquals("sweet", component.getMyModule("sweet").name);
    }

}
