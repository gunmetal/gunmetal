package com.github.overengineer.container;

import com.github.overengineer.container.key.ClassKey;
import com.github.overengineer.container.key.Key;
import com.github.overengineer.container.key.Locksmith;
import com.github.overengineer.container.proxy.HotSwapException;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import dagger.ObjectGraph;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.behaviors.Caching;
import org.picocontainer.containers.TransientPicoContainer;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import se.jbee.inject.Dependency;
import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.util.Scoped;

import org.junit.Test;

import static com.github.overengineer.container.DefaultContainerTest.*;

/**
 */
public class BenchMarks {

    int threads = 4;
    long duration = 5000;
    long primingRuns = 10000;

    private void printComparison(long mine, long theirs, String theirName) {
        System.out.println(mine/(theirs * 1.0d) + " times faster than " + theirName);
    }

    @Test
    public void testContainerCreationSpeed() throws Exception {

        final Key<IBean> key = Locksmith.makeKey(IBean.class, com.github.overengineer.container.key.Qualifier.NONE);

        long mines = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                Clarence.please().gimmeThatTainer()
                        .add(IBean.class, Bean.class)
                        .add(IBean2.class, Bean2.class)
                        .get(key);
            }
        }, threads).run(duration, primingRuns, "my container creation");

        long daggers = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                ObjectGraph.create(new DaggerIBeanModule()).get(IBean.class);
            }
        }, threads).run(duration, primingRuns, "dagger container creation");

        long picos = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                new TransientPicoContainer()
                        .addComponent(IBean.class, Bean.class)
                        .addComponent(IBean2.class, Bean2.class)
                        .getComponent(IBean.class);
            }
        }, threads).run(duration, primingRuns, "pico container creation");

        long guices = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                Guice.createInjector(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(IBean.class).to(Bean.class);
                        bind(IBean2.class).to(Bean2.class);
                    }
                }).getInstance(IBean.class);
            }
        }, threads).run(duration, primingRuns, "guice container creation");

        long springs = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {

                DefaultListableBeanFactory applicationContext = new DefaultListableBeanFactory();

                GenericBeanDefinition beanDefinition2 = new GenericBeanDefinition();
                beanDefinition2.setBeanClass(Bean2.class);
                applicationContext.registerBeanDefinition("bean2", beanDefinition2);

                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(Bean.class);
                ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
                constructorArgs.addIndexedArgumentValue(0, applicationContext.getBean("bean2"));

                beanDefinition.setConstructorArgumentValues(constructorArgs);
                applicationContext.registerBeanDefinition("bean", beanDefinition);

                applicationContext.getBean("bean");

            }
        }, threads).run(duration, primingRuns, "spring container creation");

        long silks = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                Bootstrap.injector(PrototypeSilkBeans.class).resolve(Dependency.dependency(IBean.class));
            }
        }, threads).run(duration, primingRuns, "silk container creation");

        printComparison(mines, picos, "pico");
        printComparison(mines, guices, "guice");
        printComparison(mines, springs, "spring");
        printComparison(mines, silks, "silks");
        printComparison(mines, daggers, "dagger");
    }

    public static class PrototypeSilkBeans extends BinderModule {
        @Override
        protected void declare() {
            per(Scoped.INJECTION).bind(IBean.class).to(Bean.class);
            per(Scoped.INJECTION).bind(IBean2.class).to(Bean2.class);
        }
    }

    @Test
    public void testPlainPrototypingSpeed() throws Exception {

        final Key<IBean> key = new ClassKey<IBean>(IBean.class);

        final Container container3 = Clarence.please().gimmeThatTainer()
                .add(IBean.class, Bean.class)
                .add(IBean2.class, Bean2.class);

        long mines = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                container3.get(key).stuff();
            }
        }, threads).run(duration, primingRuns, "my plain prototype");


        final ObjectGraph objectGraph = ObjectGraph.create(new DaggerIBeanModule());

        long daggers = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                objectGraph.get(IBean.class).stuff();
            }
        }, threads).run(duration, primingRuns, "dagger plain prototype");

        final MutablePicoContainer picoContainer = new DefaultPicoContainer()
                .addComponent(IBean.class, Bean.class)
                .addComponent(IBean2.class, Bean2.class);

        long picos = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                picoContainer.getComponent(IBean.class).stuff();
            }
        }, threads).run(duration, primingRuns, "pico plain prototype");



        final Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IBean.class).to(Bean.class);
                bind(IBean2.class).to(Bean2.class);
            }
        });

        final com.google.inject.Key<IBean> gKey = injector.getBinding(IBean.class).getKey();

        long guices = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                injector.getInstance(gKey).stuff();
            }
        }, threads).run(duration, primingRuns, "guice plain prototypes");

        final ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-plain-prototype.xml");

        long springs = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                applicationContext.getBean(Bean.class).stuff();
            }
        }, threads).run(duration, primingRuns, "spring plain prototypes");

        final se.jbee.inject.Injector silk = Bootstrap.injector(PrototypeSilkBeans.class);
        final Dependency<IBean> iBeanDependency = Dependency.dependency(IBean.class);

        long silks = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                silk.resolve(iBeanDependency).stuff();
            }
        }, threads).run(duration, primingRuns, "silk container creation");

        printComparison(mines, picos, "pico");
        printComparison(mines, guices, "guice");
        printComparison(mines, springs, "spring");
        printComparison(mines, silks, "silks");
    }

    public static class SingletonSilkBeans extends BinderModule {
        @Override
        protected void declare() {
            per(Scoped.APPLICATION).bind(ISingleton.class).to(Singleton.class);
            per(Scoped.APPLICATION).bind(ISingleton2.class).to(Singleton2.class);
        }
    }

    @org.junit.Test
    public void testSingletonSpeed() throws Exception {


        final Key<ISingleton> key = new ClassKey<ISingleton>(ISingleton.class);

        final Container container2 = Clarence.please().gimmeThatTainer()
                .add(ISingleton.class, Singleton.class)
                .add(ISingleton2.class, Singleton2.class)
                .getReal();

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                container2.get(ISingleton.class).yo();
                container2.get(ISingleton2.class).yo();
                container2.get(ISingleton.class).yo();
                container2.get(ISingleton2.class).yo();
                container2.get(ISingleton.class).yo();
                container2.get(ISingleton2.class).yo();
                container2.get(ISingleton.class).yo();
                container2.get(ISingleton2.class).yo();
            }
        }, threads).run(duration, primingRuns, "my singleton");

        long mines = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                container2.get(key).yo();
            }
        }, threads).run(duration, primingRuns, "my singleton");

        final ObjectGraph objectGraph = ObjectGraph.create(new DaggerISingletonModule());

        long daggers = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                objectGraph.get(ISingleton.class).yo();
            }
        }, threads).run(duration, primingRuns, "dagger singleton");


        final PicoContainer picoContainer3 = new DefaultPicoContainer(new Caching())
                .addComponent(ISingleton.class, Singleton.class)
                .addComponent(ISingleton2.class, Singleton2.class);

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                picoContainer3.getComponent(ISingleton.class).yo();
                picoContainer3.getComponent(ISingleton2.class).yo();
                picoContainer3.getComponent(ISingleton.class).yo();
                picoContainer3.getComponent(ISingleton2.class).yo();
                picoContainer3.getComponent(ISingleton.class).yo();
                picoContainer3.getComponent(ISingleton2.class).yo();
                picoContainer3.getComponent(ISingleton.class).yo();
                picoContainer3.getComponent(ISingleton2.class).yo();
            }
        }, threads).run(duration, primingRuns, "pico singleton");

        long picos = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                picoContainer3.getComponent(ISingleton.class).yo();
            }
        }, threads).run(duration, primingRuns, "pico singleton");




        final Injector injector3 = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ISingleton.class).to(Singleton.class).in(Scopes.SINGLETON);
                bind(ISingleton2.class).to(Singleton2.class).in(Scopes.SINGLETON);
            }
        });

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                injector3.getInstance(ISingleton.class).yo();
                injector3.getInstance(ISingleton2.class).yo();
                injector3.getInstance(ISingleton.class).yo();
                injector3.getInstance(ISingleton2.class).yo();
                injector3.getInstance(ISingleton.class).yo();
                injector3.getInstance(ISingleton2.class).yo();
                injector3.getInstance(ISingleton.class).yo();
                injector3.getInstance(ISingleton2.class).yo();
            }
        }, threads).run(duration, primingRuns, "guice singleton");

        long guices = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                injector3.getInstance(ISingleton.class).yo();
            }
        }, threads).run(duration, primingRuns, "guice singleton");

        final ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-singleton.xml");

        long springs = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                applicationContext.getBean(Singleton.class).yo();
            }
        }, threads).run(duration, primingRuns, "spring singleton");

        final se.jbee.inject.Injector silk = Bootstrap.injector(SingletonSilkBeans.class);
        final Dependency<ISingleton> iSingletonDependency = Dependency.dependency(ISingleton.class);

        long silks = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                silk.resolve(iSingletonDependency).yo();
            }
        }, threads).run(duration, primingRuns, "silk singleton");

        printComparison(mines, picos, "pico");
        printComparison(mines, guices, "guice");
        printComparison(mines, springs, "spring");
        printComparison(mines, silks, "silks");

    }

    @Test
    public void testCyclicRefSpeed() throws Exception {

        final Container container = Clarence.please().gimmeThatProxyTainer()
                .add(ICyclicRef.class, PCyclicTest.class)
                .add(ICyclicRef2.class, CyclicTest2.class)
                .add(ICyclicRef3.class, CyclicTest3.class);

        long mines= new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                container.get(ICyclicRef2.class);
            }
        }, threads).run(duration, primingRuns, "my cyclic refs");

        final Injector injector2 = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ICyclicRef.class).to(PCyclicTest.class);
                bind(ICyclicRef2.class).to(CyclicTest2.class);
                bind(ICyclicRef3.class).to(CyclicTest3.class);
            }
        });

        long guices = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() throws HotSwapException {
                injector2.getInstance(ICyclicRef2.class);
            }
        }, threads).run(duration, primingRuns, "guice cyclic refs");

        printComparison(mines, guices, "guice");

    }
}
