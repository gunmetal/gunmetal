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

import io.gunmetal.FromModule;
import io.gunmetal.Inject;
import io.gunmetal.Lazy;
import io.gunmetal.Module;
import io.gunmetal.MultiBind;
import io.gunmetal.Overrides;
import io.gunmetal.Param;
import io.gunmetal.Ref;
import io.gunmetal.Singleton;
import io.gunmetal.Supplies;
import io.gunmetal.internal.ComponentTemplate;
import io.gunmetal.sandbox.testmocks.A;
import io.gunmetal.sandbox.testmocks.AA;
import io.gunmetal.sandbox.testmocks.F;
import io.gunmetal.sandbox.testmocks.NewGunmetalBenchMarkModule;
import io.gunmetal.spi.Converter;
import io.gunmetal.spi.ConverterSupplier;
import io.gunmetal.spi.GunmetalComponent;
import io.gunmetal.spi.Option;
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
import java.util.function.Supplier;

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

    @Module(notAccessibleFrom = TestModule.BlackList.class,
            dependsOn = StatefulModule.class,
            type = Module.Type.STATELESS)
    public static class TestModule {

        @Supplies @Singleton @Overrides(allowCycle = true) static Bad providedCirc(Supplier<Circ> circProvider) {
            return circProvider.get();
        }

        @Supplies @Overrides(allowCycle = true) static Circ circ() {
            return new Circ();
        }

        @io.gunmetal.BlackList(M.class)
        static class BlackList {

            @Inject
            TestModule testModule;


        }


        @Supplies @Singleton static Class<Void> routine(BlackList blackList) {
            System.out.println("routineeee" + blackList);
            return Void.TYPE;
        }


        @Supplies @Singleton @Overrides(allowMappingOverride = true) static TestModule tm(ArrayList<Integer> integers) {
            return new TestModule();
        }

        @Supplies @Singleton static TestModule tmO(@Param String name) {
            System.out.println(name);
            return new TestModule();
        }

        @Supplies @Singleton @Main static SandboxIntegrationTest test(SandboxIntegrationTest test) {
            return new SandboxIntegrationTest();
        }

        @Supplies @Singleton static Object test2(TestModule testModule, Supplier<SandboxIntegrationTest> test, BlackList blackList) {
            System.out.println(test.get());
            System.out.println(test.get());

            System.out.println(blackList);
            return test.get();
        }

        @Supplies static SandboxIntegrationTest testy() {
            return new SandboxIntegrationTest();
        }

        @Supplies @Singleton @MultiBind static String s1(@Stateful String name) {
            return name;
        }

        @Supplies @Singleton @MultiBind static String s2() {
            return "2";
        }

        @Supplies @Singleton @MultiBind static String s3() {
            return "3";
        }

        @Supplies @Singleton @Main static InputStream printStrings(@MultiBind List<String> strings) {
            for (String s : strings) {
                System.out.println(s);
            }
            return System.in;
        }

        @Supplies @Singleton @Lazy static List<ProvisionStrategyDecorator> decorators() {
            return Collections.singletonList((resourceMetadata, delegateStrategy, linkers) -> delegateStrategy);
        }

        @Supplies @Singleton @Lazy static Map<? extends Scope, ? extends ProvisionStrategyDecorator> scopeDecorators() {
            return Collections.singletonMap(
                    CustomScopes.TEST,
                    (resourceMetadata, delegateStrategy, linkers) -> delegateStrategy);
        }

    }

    @Module(type = Module.Type.COMPONENT_PARAM)
    @Stateful
    @Singleton
    public static class StatefulModule {

        String name;

        StatefulModule(String name) {
            this.name = name;
        }

        @Supplies @Singleton String name(SandboxIntegrationTest test) {
            return name + test.getClass().getName();
        }

        @Supplies @Singleton List<StatefulModule> statefulModules(@FromModule Ref<StatefulModule> statefulModuleRef) {
            assert statefulModuleRef.get() == statefulModuleRef.get();
            return Collections.singletonList(statefulModuleRef.get());
        }

    }

    @Module
    static class M {
        @Supplies @Singleton @Lazy static M m(SandboxIntegrationTest test) {
            return new M();
        }
    }

    enum CustomScopes implements Scope {
        TEST;

        @Override public boolean canInject(Scope scope) {
            return scope.equals(scope);
        }
    }

    @Module(dependsOn = TestModule.class, type = Module.Type.COMPONENT)
    public interface TestComponent {

        void inject(Object o);

        TestModule t(@Param String name);

        @Supplies SandboxIntegrationTest sandboxIntegrationTest();

        public interface Factory {
            TestComponent create(StatefulModule statefulModule);
        }

    }

    @Module(dependsOn = NewGunmetalBenchMarkModule.class, type = Module.Type.COMPONENT)
    public interface GComponent {

        void inject(Object o);

        public interface Factory {
            GComponent create();
        }

    }

    @Test
    public void testBuild() {

        TestComponent.Factory templateGraph = ComponentTemplate
                .build(
                        new GunmetalComponent.Default(Option.REQUIRE_ACYCLIC),
                        TestComponent.Factory.class);
        templateGraph.create(new StatefulModule("rees"));

        TestComponent app = templateGraph.create(new StatefulModule("rees"));

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
        app.inject(badDep);
        app.inject(badDep2);
        app.inject(badDep3);
        app.inject(badDep4);

        assert badDep.circ != badDep4.circ;

        BadDep badDep5 = new BadDep();
        app.inject(badDep5);
        assert (badDep.circ).providedCirc != badDep5.circ;

        assert test != this;

        class Dep2 {
            @Inject A a;
        }

        Dep2 dep2 = new Dep2();

        GComponent gApp = ComponentTemplate
                .build(GComponent.Factory.class)
                .create();

        gApp.inject(dep2);
        A a = dep2.a;

        gApp.inject(dep2);
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

        gApp.inject(injectTest);

        gApp.inject(new InjectTest2());

        assert injectTest.f != null;

    }

    @Module(dependsOn = {TestModule.class, M.class}, type = Module.Type.COMPONENT)
    public interface BadComponent {

        public interface Factory {
            BadComponent create();
        }

    }

    @Test(expected = RuntimeException.class)
    public void testBlackList() {
        ComponentTemplate.build(BadComponent.Factory.class);
    }

    @Module(subsumes = MyLibrary.class, type = Module.Type.PROVIDED)
    @Main
    public static class PlusModule implements Cheese {

        @Inject SandboxIntegrationTest sandboxIntegrationTest;

        @Supplies @Singleton static PlusModule plusModule() {
            return new PlusModule();
        }

        @Supplies @Singleton static Cheese cheese() {
            return new PlusModule();
        }

        @Supplies @Singleton static OutputStream r(@FromModule Cheese cheese, @Main Lib myLibrary) {
            System.out.println("sup");
            return System.out;
        }

    }

    interface Lib {
    }

    @Module(lib = true)
    static class MyLibrary implements Lib {

        @Supplies @Singleton static Lib huh(@FromModule Cheese cheese) {
            System.out.println("sup library");
            return new MyLibrary();
        }

    }

    public interface Cheese {
    }

    @Module(dependsOn = PlusModule.class, type = Module.Type.COMPONENT)
    public interface PlusComponent {

        void inject(Object o);

        public interface Factory {
            PlusComponent create(TestComponent parent);
        }

    }

    @Test
    public void testPlus() {


        class Dep {
            @Inject @Main PlusModule plusModule;
        }

        Dep dep = new Dep();

        TestComponent parent = ComponentTemplate.build(TestComponent.Factory.class)
                .create(new StatefulModule("plus"));

        PlusComponent.Factory childTemplate = ComponentTemplate.build(PlusComponent.Factory.class);
        PlusComponent child = childTemplate.create(parent);

        child.inject(dep);
        PlusModule p = dep.plusModule;

        assert p.sandboxIntegrationTest != null;

        dep.plusModule = null;

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

        PlusComponent childCopy = childTemplate.create(parent);
        Dep dep2 = new Dep();
        child.inject(dep);
        childCopy.inject(dep2);
        assert dep.plusModule != dep2.plusModule;

        child.inject(dep2);
        assert dep.plusModule == dep2.plusModule;

        childCopy.inject(injectTest);

    }

    static class AaHolder {
        @Inject AA aa;
    }

    @Module static class ConversionModule {

        @Inject Long numberLong;

        @Supplies static String numberString() {
            return "3425";
        }

    }

    @Module(dependsOn = ConversionModule.class, type = Module.Type.COMPONENT)
    public interface ConversionComponent {

        void inject(Object o);

        public interface Factory {
            ConversionComponent create();
        }

    }

    @Test
    public void testConversion() {

        ConverterSupplier converterSupplier = to -> {
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
        };

        GunmetalComponent gunmetalComponent = new GunmetalComponent.Default() {

            @Override public ConverterSupplier converterSupplier() {
                return converterSupplier;
            }

        };

        ConversionComponent graph =
                ComponentTemplate.build(gunmetalComponent, ConversionComponent.Factory.class).create();

        ConversionModule c = new ConversionModule();
        graph.inject(c);

        assertEquals(3425L, (long) c.numberLong);
    }

    @Module
    public static class MyModule {
        String name;
        @Supplies static MyModule myModule(
                @Param String name,
                @MultiBind Supplier<List<ProvisionStrategyDecorator>> provider,
                @MultiBind Ref<List<ProvisionStrategyDecorator>> ref) {
            MyModule m = new MyModule();
            m.name = name;
            return m;
        }
    }

    @Module(dependsOn = MyModule.class, type = Module.Type.COMPONENT)
    public interface MyComponent {

        MyModule getMyModule(@Param String name);

        public interface Factory {
            MyComponent create();
        }
    }

    @Test
    public void testComponent() {
        MyComponent component = ComponentTemplate
                .build(MyComponent.Factory.class)
                .create();
        assertEquals("sweet", component.getMyModule("sweet").name);
    }

    @Module
    public static class Bullshit {

        @Supplies static Bullshit bs(@Param String word) {
            System.out.println(word);
            return new Bullshit();
        }

        @Supplies @Singleton static String bs() {
            return "test";
        }

    }

    @Module(dependsOn = Bullshit.class, type = Module.Type.COMPONENT)
    public interface BullshitComponent {
        Bullshit bs(@Param String word);
        public static interface Factory {
            BullshitComponent bullshitComponent();
        }
    }

    @Test
    public void testBullShit() {
        ComponentTemplate
                .build(BullshitComponent.Factory.class)
                .bullshitComponent()
                .bs("what");
    }

}
