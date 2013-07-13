package com.github.overengineer.gunmetal;

import com.github.overengineer.gunmetal.key.Generic;
import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.metadata.*;
import com.github.overengineer.gunmetal.module.BaseModule;
import com.github.overengineer.gunmetal.proxy.HotSwapException;
import com.github.overengineer.gunmetal.proxy.HotSwappableContainer;
import com.github.overengineer.gunmetal.proxy.aop.AopContainer;
import com.github.overengineer.gunmetal.proxy.aop.Aspect;
import com.github.overengineer.gunmetal.proxy.aop.JoinPoint;
import com.github.overengineer.gunmetal.proxy.aop.Pointcut;
import com.github.overengineer.gunmetal.scope.ScopedComponentStrategyProvider;
import com.github.overengineer.gunmetal.testutil.ConcurrentExecutionAssistant;
import com.github.overengineer.gunmetal.testutil.SerializationTestingUtil;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.ProvisionException;
import org.junit.Test;
import scope.CommonConstants;
import scope.CommonModule;
import scope.monitor.DefaultSchedulerProvider;
import scope.monitor.ScheduledExecutorTimeoutMonitor;
import scope.monitor.SchedulerProvider;
import scope.monitor.TimeoutMonitor;

import java.io.Serializable;
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class DefaultContainerTest implements Serializable {

    int threads = 4;
    long duration = 1000;
    long primingRuns = 10000;

    @Test
    public void testLoadModule() {

        assertNotNull(
                Gunmetal.create(new CommonModule()).get(TimeoutMonitor.class));

    }

    @Test
    public void testSerialization() {

        AopContainer container = Gunmetal.raw().withSetterInjection().gimmeThatAopTainer();

        container.add(IBean.class, Bean3.class);

        container.add(IBean2.class, Bean2.class);

        container.addInstance(new Generic<List<Integer>>() {}, new ArrayList<Integer>());

        container.registerDeconstructedApi(StartListener.class);

        container.registerCompositeTarget(StartListener.class);

        container.loadModule(CommonModule.class);

        container
                .addAspect(TestAspect.class)
                .addAspect(Metaceptor.class);

        List<String> strings = new ArrayList<String>();

        container.addInstance(new Generic<List<String>>() {}, strings);

        container = SerializationTestingUtil.getSerializedCopy(container);

        TimeoutMonitor monitor = container.get(TimeoutMonitor.class);

        assertNotNull(monitor);

        assertEquals(strings, container.get(new Generic<List<String>>(){}));

        assertNotNull(container.get(IBean.class));
    }

    @Test
    public void testVerify_positive() throws WiringException {

        Container container = Gunmetal.raw().load();

        container.verify();

        container.loadModule(CommonModule.class);

        container.verify();

    }

    @Test(expected = WiringException.class)
    public void testVerify_negative() throws WiringException {

        Container container = Gunmetal.raw().withSetterInjection().load();

        container.add(TimeoutMonitor.class, ScheduledExecutorTimeoutMonitor.class);

        container.addInstance(Long.class, CommonConstants.Properties.MONITORING_FREQUENCY, 4L);

        container.verify();

    }

    @Test
    public void testAddChild() {

        Container master = Gunmetal.raw().load();

        Container common = Gunmetal.raw().load();

        Container sibling = Gunmetal.raw().load();

        common.loadModule(CommonModule.class);

        master.addChild(common);

        master.addChild(sibling);

        assertNotNull(common.get(TimeoutMonitor.class));

        assertNotNull(master.get(TimeoutMonitor.class));

        try {
            assertNull(sibling.get(TimeoutMonitor.class));
        } catch (MissingDependencyException e) {
             //sup
        }

        Container cascadeFuck = Gunmetal.raw().load();

        master.addCascadingContainer(cascadeFuck);

        try {
            cascadeFuck.addChild(common);
        } catch (CircularReferenceException e) {

        }

        cascadeFuck.loadModule(CommonModule.class);

        assertNotNull(sibling.get(TimeoutMonitor.class));

        try {
            common.get(TestInterceptor2.class);
        } catch (MissingDependencyException e) {
            //sup
        }

        Container global = Gunmetal.raw().load();

        global.add(ISingleton.class, Singleton.class);

        master.addCascadingContainer(global);

        sibling.get(ISingleton.class);

        try {
            master.get(TestInterceptor2.class);
        } catch (MissingDependencyException e) {
            //sup
        }

    }

    @Pointcut(classes = {IBean.class, ISingleton.class})
    public static class TestInterceptor2 implements Aspect {
        @Override
        public Object advise(JoinPoint invocation) throws Throwable {
            return invocation.join();
        }
    }

    @Test
    public void testAddAndGetComponent() {

        Container container = Gunmetal.raw().load();

        container.add(SchedulerProvider.class, Bro.class, DefaultSchedulerProvider.class);

        container.addInstance(Integer.class, CommonConstants.Properties.MONITORING_THREAD_POOL_SIZE, 4);

        SchedulerProvider provider = container.get(SchedulerProvider.class);

        assertTrue(provider instanceof DefaultSchedulerProvider);

        container.add(IConstructorTest.class, ConstructorTest.class);

        assertEquals(provider, ((ConstructorTest) container.get(IConstructorTest.class)).provider);

    }

    @Test
    public void testAddAndGetInstance() {

        Container container = Gunmetal.raw().load();

        SchedulerProvider given = new DefaultSchedulerProvider();

        container.addInstance(SchedulerProvider.class, given);

        container.addInstance(Integer.class, CommonConstants.Properties.MONITORING_THREAD_POOL_SIZE, 4);

        SchedulerProvider received = container.get(SchedulerProvider.class);

        assertEquals(given, received);

    }

    @Test
    public void testAddAndGetProperty() {

        Container container = Gunmetal.raw().gimmeThatAopTainer();

        container.addInstance(Long.class, "test", 69L);

        assertEquals((Long) 69L, container.get(Long.class, "test"));

        container.get(ComponentStrategyFactory.class);

    }

    @Test
    public void testAddAndGetGeneric() {

        Container container = Gunmetal.raw().load();

        List<? extends String> strings = new ArrayList<String>();

        List<Integer> integers = new ArrayList<Integer>();

        container.addInstance(new Generic<List<? extends String>>("strings"){}, strings);

        container.addInstance(new Generic<List<Integer>>(){}, integers);

        container.add(ListBoy.class, ListBoy.class);

        assertEquals(strings, container.get(new Generic<List<? extends String>>("strings") {
        }));

        assertEquals(integers, container.get(new Generic<List<Integer>>(){}));

        container.get(ListBoy.class);

    }

    public static class ListBoy {
        ListBoy(@com.github.overengineer.gunmetal.metadata.Named("strings") List<? extends String> strings) {
            System.out.println(strings);
        }
    }

    @Test
    public void testRegisterFactory() {

        Container container = Gunmetal.raw().load();

        container.loadModule(CommonModule.class);

        com.github.overengineer.gunmetal.metadata.Provider<TimeoutMonitor> timeoutMonitorFactory
                = container.get(new Generic<com.github.overengineer.gunmetal.metadata.Provider<TimeoutMonitor>>(){});

        assert timeoutMonitorFactory.get() != null;

        container.add(IConstructorTest.class, FactoryTest.class);

        IConstructorTest i = container.get(IConstructorTest.class);

        assertEquals(timeoutMonitorFactory, ((FactoryTest) i).timeoutMonitorFactory);

        System.out.println(timeoutMonitorFactory);

        container.add(new Generic<com.github.overengineer.gunmetal.metadata.Provider<TimeoutMonitor>>(){}, FactoryTest.class);

        container = SerializationTestingUtil.getSerializedCopy(container);

        timeoutMonitorFactory = container.get(new Generic<com.github.overengineer.gunmetal.metadata.Provider<TimeoutMonitor>>(){});

        assert timeoutMonitorFactory.get() != null;

        assert timeoutMonitorFactory instanceof FactoryTest;

    }

    @Test
    public void testNonManagedComponentFactory() {

        Container container = Gunmetal.raw().load();

        Dependency<NonManagedComponentFactory<NamedComponent>> factoryKey = new Generic<NonManagedComponentFactory<NamedComponent>>() {};

        container.registerNonManagedComponentFactory(factoryKey, NonManagedComponent.class);

        container.loadModule(CommonModule.class);

        container = SerializationTestingUtil.getSerializedCopy(container);

        NonManagedComponentFactory<NamedComponent> namedComponentFactory = container.get(factoryKey);

        assertEquals("test", namedComponentFactory.create("test").getName());

    }

    public interface NonManagedComponentFactory<T extends NamedComponent> {
        T create(String name);
    }

    public interface NamedComponent {
        String getName();
    }

    public static class NonManagedComponent implements NamedComponent {
        String name;
        public NonManagedComponent(String name, TimeoutMonitor timeoutMonitor) {
            this.name = name;
        }
        @Override
        public String getName() {
            return name;
        }
    }

    public static class FactoryTest implements IConstructorTest, com.github.overengineer.gunmetal.metadata.Provider<TimeoutMonitor>, Serializable {

        com.github.overengineer.gunmetal.metadata.Provider<TimeoutMonitor> timeoutMonitorFactory;

        public FactoryTest(com.github.overengineer.gunmetal.metadata.Provider<TimeoutMonitor> timeoutMonitorFactory) {
            this.timeoutMonitorFactory = timeoutMonitorFactory;
        }

        @Override
        public TimeoutMonitor get() {
            return timeoutMonitorFactory.get();
        }
    }


    @Test(expected = Assertion.class)
    public void testAddListener() throws Throwable {

        Gunmetal.raw().withSetterInjection().load().makeInjectable()
                .addPostProcessor(Listener.class)
                .add(SchedulerProvider.class, DefaultSchedulerProvider.class)
                .addInstance(Integer.class, CommonConstants.Properties.MONITORING_THREAD_POOL_SIZE, 4)
                .get(SchedulerProvider.class);


    }

    public static class Listener implements ComponentPostProcessor {

        public void postConstruct(Container container) {
            System.out.println("what up" + container);
        }

        @Override
        public <T> T postProcess(T component) {
            throw new Assertion();
        }
    }

    @Test(expected = Assertion.class)
    public void testIntercept() throws HotSwapException {

        AopContainer c = Gunmetal.raw().gimmeThatAopTainer().makeInjectable().get(AopContainer.class);

        c.loadModule(CommonModule.class);

        c.addAspect(TestAspect.class).addAspect(Metaceptor.class);
        c.addInstance(SchedulerProvider.class, new DefaultSchedulerProvider());
        c.add(ICyclicRef3.class, CyclicTest3.class);
        c.get(ComponentStrategyFactory.class);
    }

    @Pointcut(
            paramterTypes = {Class.class, Object.class},
            annotations = {},
            classes = {},
            classNameExpression = "*github.overengineer*",
            methodNameExpression = "add*",
            returnType = Object.class
    )
    public static class TestAspect implements Aspect {

        public TestAspect(TimeoutMonitor monitor) {

        }

        int i = 0;

        @Override
        public Object advise(JoinPoint invocation) throws Throwable {
            System.out.println(this);
            if (i > 0) throw new Assertion();
            i++;
            Object result = invocation.join();
            System.out.println(this);
            return result;
        }
    }

    @Pointcut(classes = Aspect.class)
    public static class Metaceptor implements Aspect {

        @Override
        public Object advise(JoinPoint invocation) throws Throwable {
            System.out.println("METACEPTOR, ATTACK!!!!");
            return invocation.join();
        }
    }



    @Test
    public void testNewEmptyClone() {
        final Container container = Gunmetal.raw().makeYourStuffInjectable().load()
                .addPostProcessor(L.class);

        //assert container.newEmptyClone().getAllComponents().size() == 1;
    }

    public static class L implements ComponentPostProcessor {
        @Override
        public <T> T postProcess(T component) {
            return component;
        }
    }

    @Test
    public void testAddCustomProvider() throws Exception {
        final Container container = Gunmetal.raw().load().makeInjectable()
                .addCustomProvider(ProvidedType.class, Provider.class);

        assert container.get(ProvidedType.class) != null;

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                container.get(ProvidedType.class);
            }
        }, threads).run(duration, primingRuns, "custom provider gets");
    }

    public static class Provider {
        ProvidedType get(Container container) {
            //System.out.println(container);
            return new ProvidedType();
        }
    }

    public static class ProvidedType {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @com.github.overengineer.gunmetal.metadata.Qualifier
    public @interface Bro {
    }

    @Bro
    public static class BroListener implements StartListener {
        @Override
        public void onStart(String processName) {
            System.out.println("bro magic");
        }
    }

    @Test
    public void testComposite() {

        final AtomicInteger calls = new AtomicInteger();

        Gunmetal.raw().withSetterInjection().gimmeThatAopTainer()
                .addAspect(StartAspect.class)
                .addInstance(StartListener.class, new StartListener() {

                    @Inject
                    @Bro
                    final StartListener bro = null;

                    @Inject
                    public void setMaster(@Bro StartListener bro) {
                        System.out.println("bro " + bro);
                        //this.bro = bro;
                    }

                    @Override
                    public void onStart(String processName) {
                        System.out.println("1 got " + processName);
                        bro.onStart(processName);
                        calls.incrementAndGet();
                    }
                })
                .registerCompositeTarget(StartListener.class)
                .addInstance(StartListener.class, new StartListener() {
                    @Override
                    public void onStart(String processName) {
                        System.out.println("2 got " + processName);
                        calls.incrementAndGet();
                    }
                })
                .registerDeconstructedApi(StartListener.class)
                .add(StartDelegate.class, StartDelegate.class)
                .addInstance(StartListener.class, new StartListener() {
                    @Override
                    public void onStart(String processName) {
                        System.out.println("3 got " + processName);
                        calls.incrementAndGet();
                        assert processName.equals("what up");
                    }
                })
                .add(StartListener.class, BroListener.class)
                .get(StartListener.class)
                .onStart("what up");

        assert calls.get() == 3;
    }

    @Pointcut(classes = StartListener.class, methodNameExpression = "onStart")
    public static class StartAspect implements Aspect {
        @Override
        public Object advise(JoinPoint joinPoint) throws Throwable {
            System.out.println("start advice started!!!" + joinPoint.getParameters()[0]);
            Object result = joinPoint.join();
            System.out.println("start advice done!!!");
            return result;
        }
    }

    public static interface StartListener {
        @ImplementedBy(StartDelegate.class)
        void onStart(String processName);
    }

    @Test
    public void testServiceDelegate() throws Exception {

        final StartListener startListener = Gunmetal.raw()
                .makeYourStuffInjectable().load()
                .registerDeconstructedApi(StartListener.class)
                .add(StartDelegate.class, StartDelegate.class)
                .get(StartListener.class);

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
               startListener.onStart("yo");
            }
        }, 4).run(1000, 1000000, "custom provider gets");

    }

    public static class StartDelegate {
        public void onStart(String processName, StartListener listener, StartListener listener2, Container container) {
            /*System.out.println("delegate got name [" + processName + "] and even got it's momma - " + listener);
            if (processName.equals("yo")) {
                listener.onStart("shit");
            }*/
        }
    }

    @Test
    public void testScopeProvider() throws Exception {

        final Container container = Gunmetal.raw().makeYourStuffInjectable().load();

        MetadataAdapter metadataAdapter = container.get(MetadataAdapter.class);

        metadataAdapter.addScope(CustomScopes.Thread, ThreadScoped.class, new MemoryLeakingThreadStrategyProvider());

        container.add(ThreadVar.class, ThreadVar.class);

        final Set<ThreadVar> threadVars = new HashSet<ThreadVar>();

        int numThreads = 7;

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                threadVars.add(container.get(ThreadVar.class));
            }
        }, numThreads).run(5000, 1000000, "custom scope shit");

        assert threadVars.size() == numThreads + 1;

    }

    @ThreadScoped
    public static class ThreadVar {}

    public static class MemoryLeakingThreadStrategyProvider implements ScopedComponentStrategyProvider {

        @Override
        public <T> ComponentStrategy<T> get(final Class<T> implementationType, final Object qualifier, final ComponentStrategy<T> delegateStrategy) {
            return new ComponentStrategy<T>() {

                ThreadLocal<T> threadLocal = new ThreadLocal<T>();

                @Override
                public T get(InternalProvider provider, ResolutionContext resolutionContext) {
                    T t = threadLocal.get();
                    if (t == null) {
                        t = delegateStrategy.get(provider, resolutionContext);
                        threadLocal.set(t);
                    }
                    return t;
                }

                @Override
                public Class getComponentType() {
                    return implementationType;
                }

                @Override
                public boolean isDecorator() {
                    return delegateStrategy.isDecorator();
                }

                @Override
                public Object getQualifier() {
                    return qualifier;
                }
            };
        }
    }

    public static enum CustomScopes implements com.github.overengineer.gunmetal.scope.Scope {
        Thread
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Scope
    public @interface ThreadScoped {}

    @Test
    public void testCollection() {

        assert
                Gunmetal.raw().load()
                    .addInstance(StartListener.class, new StartListener() {
                        @Override
                        public void onStart(String processName) {
                            System.out.println("1 got " + processName);
                        }
                    })
                    .addInstance(StartListener.class, "zippy", new StartListener() {
                        @Override
                        public void onStart(String processName) {
                            System.out.println("2 got " + processName);
                        }
                    })
                    .addInstance(StartListener.class, "zippy", new StartListener() {
                        @Override
                        public void onStart(String processName) {
                            System.out.println("3 got " + processName);
                        }
                    })
                    .add(ListenerHolder.class, ListenerHolder.class)
                    .get(ListenerHolder.class).getListeners().size() == 2;

    }

    public static class ListenerHolder {
        Set<StartListener> listeners;
        ListenerHolder(@com.github.overengineer.gunmetal.metadata.Named("zippy") Set<StartListener> listeners) {
            System.out.println(listeners.size());
             this.listeners = listeners;
        }
        Set<StartListener> getListeners() {
            return listeners;
        }
    }

    interface IConstructorTest {}

    @Prototype
    public static class ConstructorTest implements IConstructorTest {
        SchedulerProvider provider;
        public ConstructorTest(SchedulerProvider provider) {
            this.provider = provider;
        }
    }



    @Test
    public void testCyclicRef2() {

        HotSwappableContainer container = Gunmetal.raw().gimmeThatProxyTainer();

        container
                .add(ICyclicRef.class, CyclicTest.class)
                .add(ICyclicRef2.class, CyclicTest2.class)
                .add(ICyclicRef3.class, CyclicTest3.class);

        ICyclicRef c = container.get(ICyclicRef.class);

        assertEquals(1, c.getRef().getRef().getRef().calls());

        container.get(ICyclicRef2.class);
        container.get(ICyclicRef2.class);
        container.get(ICyclicRef2.class);
    }

    @Test
    public void testHotSwapping() throws HotSwapException {

        HotSwappableContainer container = Gunmetal.raw().gimmeThatProxyTainer();

        container
                .add(ICyclicRef.class, CyclicTest.class)
                .add(ICyclicRef2.class, CyclicTest2.class)
                .add(ICyclicRef3.class, CyclicTest3.class);

        ICyclicRef c = container.get(ICyclicRef.class);

        container.swap(ICyclicRef.class, CyclicTestHot.class);

        assertEquals(69, c.calls());

    }

    @Test
    public void testCyclicRef() {

        HotSwappableContainer container = Gunmetal.raw().withSetterInjection().gimmeThatProxyTainer();

        container
                .add(ICyclicRef.class, CyclicTest.class)
                .add(ICyclicRef2.class, CyclicTest2.class)
                .add(ICyclicRef3.class, CyclicTest3.class);

        ICyclicRef c = container.get(ICyclicRef.class);

        assertNotNull(c.getRef());

        c.getRef().getRef();

        ICyclicRef2 c2 = container.get(ICyclicRef2.class);

        assertNotNull(c2.getRef());

        assertEquals(2, c2.getRef().calls());

        assertEquals(2, c2.calls());

        ICyclicRef3 c3 = container.get(ICyclicRef3.class);

        assertNotNull(c3.getRef());

        assertEquals(1, c.getRef().calls());

    }

    @Test
    public void testWTF() {

        Gunmetal.raw().withSetterInjection().gimmeThatProxyTainer()
                .add(IWTF.class, WTF.class)
                .add(IWTF2.class, WTF2.class)
                .get(IWTF.class);

    }

    public interface IWTF {}

    public interface IWTF2 {}

    public static class WTF implements IWTF {
        WTF(IWTF2 iwtf2) {

        }
    }

    public static class WTF2 implements IWTF2 {
        WTF2(IWTF iwtf) {

        }
    }

    public interface ICyclicRef {
        ICyclicRef3 getRef();
        int calls();
    }

    @Prototype
    public interface ICyclicRef2 {
        ICyclicRef getRef();
        int calls();
    }

    public interface ICyclicRef3 {
        ICyclicRef2 getRef();
        int calls();
    }

    public static class CyclicTest implements ICyclicRef {
        ICyclicRef3 cyclicTest3;
        int calls = 0;
        @com.google.inject.Inject
        public CyclicTest(ICyclicRef3 cyclicTest3) {
            cyclicTest3.calls();
            this.cyclicTest3 = cyclicTest3;
        }

        @Override
        public ICyclicRef3 getRef() {
            calls++;
            return cyclicTest3;
        }

        @Override
        public int calls() {
            return calls;
        }
    }

    public static class CyclicTestHot extends CyclicTest {
        @com.google.inject.Inject
        public CyclicTestHot(ICyclicRef3 cyclicTest3) {
            super(cyclicTest3);
        }
        @Override
        public int calls() {
            return 69;
        }
    }

    @Prototype
    public static class CyclicTest2 implements ICyclicRef2 {
        ICyclicRef cyclicTest;
        int calls = 0;
        @com.google.inject.Inject
        public CyclicTest2(ICyclicRef cyclicTest) {
            this.cyclicTest = cyclicTest;
        }

        @Override
        public ICyclicRef getRef() {
            calls++;
            return cyclicTest;
        }

        @Override
        public int calls() {
            return calls;
        }
    }

    @Prototype
    public static class CyclicTest3 implements ICyclicRef3 {
        ICyclicRef2 cyclicTest;
        int calls = 0;
        @com.google.inject.Inject
        public CyclicTest3(ICyclicRef2 cyclicTest) {
            this.cyclicTest = cyclicTest;
        }

        @Override
        public ICyclicRef2 getRef() {
            calls++;
            return cyclicTest;
        }

        @Override
        public int calls() {
            return calls;
        }
    }

    public interface ISingleton{void yo();}

    public static class Singleton implements ISingleton {
        @Override
        public void yo() {
        }
    }

    public static class Assertion extends RuntimeException {}

    @Test(expected = ProvisionException.class)
    public void testCyclicFields() {

        A a = Gunmetal.raw().withSetterInjection().load(new BaseModule() {
            @Override
            public void configure() {
            }
        }).get(A.class);

        assert a == a.b.c.d.b.c.d.a.b.c.dd.e.f.g.e.f.a;

        C c = Gunmetal.raw().withSetterInjection().load(new BaseModule() {
            @Override
            public void configure() {
            }
        }).get(C.class);

        assert c == c.b.c.d.c.dd.e.f.g.e.f.a.b.c;

        Guice.createInjector(new AbstractModule() {
            @Override
            public void configure() {
            }
        }).getInstance(A.class).b.toString();

    }

    @Prototype
    static class A {
        @Inject @javax.inject.Inject B b;
        @javax.inject.Inject
        public A(B b) {
            this.b = b;
        }
    }
    static class B {

        @javax.inject.Inject
        C c;

        @Inject
        public void setC(C c) {
            this.c = c;
        }

    }
    @Prototype
    static class C {
        @Inject @javax.inject.Inject D d;
        @Inject @javax.inject.Inject B b;
        DD dd;
        @javax.inject.Inject
        public C(D d, DD dd) {
            this.d = d;
            this.dd = dd;
        }
    }
    static class D {
        @Inject @javax.inject.Inject A a;
        @Inject @javax.inject.Inject B b;
        @Inject @javax.inject.Inject C c;
    }
    static class DD {
        @Inject @javax.inject.Inject E e;
        @javax.inject.Inject
        DD(E e) {
            this.e = e;
        }
    }
    static class E {
        @Inject @javax.inject.Inject F f;
        @javax.inject.Inject
        public E(F f) {
            this.f = f;
        }
    }
    static class F {
        @Inject @javax.inject.Inject A a;
        @Inject @javax.inject.Inject G g;
        @javax.inject.Inject
        public F(A a) {
            this.a = a;
        }
    }
    static class G {
        @Inject @javax.inject.Inject E e;
        @javax.inject.Inject
        public G(E e) {
            this.e = e;
        }
    }

}
