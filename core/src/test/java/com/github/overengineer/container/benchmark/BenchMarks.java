package com.github.overengineer.container.benchmark;

import com.github.overengineer.container.*;
import com.github.overengineer.container.benchmark.*;
import com.github.overengineer.container.key.ClassKey;
import com.github.overengineer.container.key.Key;
import com.github.overengineer.container.key.Locksmith;
import com.github.overengineer.container.key.Qualifier;
import com.github.overengineer.container.module.BaseModule;
import com.github.overengineer.container.module.Module;
import com.github.overengineer.container.proxy.HotSwapException;
import com.github.overengineer.container.scope.Scopes;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
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
    long primingRuns = 100000;

    private void printComparison(long mine, long theirs, String theirName) {
        System.out.println(mine/(theirs * 1.0d) + " times faster than " + theirName);
    }

    @Test
    public void testContainerCreationSpeed_x() throws Exception {

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                Clarence.please().withFastMetadata().gimmeThatTainer().loadModule(new BenchMarkModule()).get(AA.class);
            }
        }, threads).run(duration, primingRuns, "my container creation");

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                ObjectGraph.create(new DaggerSlimBenchMarkModule()).get(AA.class);
            }
        }, threads).run(duration, primingRuns, "dagger slim container creation");

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                ObjectGraph.create(new DaggerBenchMarkModule()).get(AA.class);
            }
        }, threads).run(duration, primingRuns, "dagger container creation");


        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                Bootstrap.injector(SilkBenchMarkModule.class).resolve(Dependency.dependency(AA.class));
            }
        }, threads).run(duration, primingRuns, "silk container creation");

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                new PicoBenchMarkModule().get().getComponent(AA.class);
            }
        }, threads).run(duration, primingRuns, "pico container creation");

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                Guice.createInjector(new GuiceBenchMarkModule()).getInstance(AA.class);
            }
        }, threads).run(duration, primingRuns, "guice container creation");

    }

    @Test
    public void testPrototypeSpeed_x() throws Exception {

        final Key<A> key = Locksmith.makeKey(A.class, Qualifier.NONE);

        final Module module = new BenchMarkModule();

        final Container container = Clarence.please().withFastMetadata().gimmeThatTainer().loadModule(module);

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                container.get(key);
            }
        }, threads).run(duration, primingRuns, "my prototype creation");

        final DaggerBenchMarkModule daggerBenchMarkModule = new DaggerBenchMarkModule();

        final ObjectGraph objectGraph = ObjectGraph.create(daggerBenchMarkModule);

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                objectGraph.get(A.class);
            }
        }, threads).run(duration, primingRuns, "dagger prototype creation");

    }

    @Test
    public void testContainerCreationSpeed() throws Exception {

        final Key<IBean> key = Locksmith.makeKey(IBean.class, Qualifier.NONE);

        final Module module = new BaseModule() {
            @Override
            protected void configure() {
                use(Bean.class).withScope(Scopes.PROTOTYPE).forType(IBean.class);
                use(Bean2.class).withScope(Scopes.PROTOTYPE).forType(IBean2.class);
            }
        };

        long mines = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                Clarence.please().withFastMetadata().gimmeThatTainer()
                        .loadModule(module)
                        .get(key);
            }
        }, threads).run(duration, primingRuns, "my container creation");

        final DaggerIBeanModule daggerIBeanModule = new DaggerIBeanModule();

        long daggers = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                ObjectGraph.create(daggerIBeanModule).get(IBean.class);
            }
        }, threads).run(duration, primingRuns, "dagger container creation");

        long picos = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                new TransientPicoContainer()
                        .addComponent(IBean.class, Bean.class)
                        .addComponent(IBean2.class, Bean2.class)
                        .getComponent(IBean.class);
            }
        }, threads).run(duration, primingRuns, "pico container creation");

        long guices = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
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
            public void execute() {

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
            public void execute() {
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
            public void execute() {
                container3.get(key).stuff();
            }
        }, threads).run(duration, primingRuns, "my plain prototype");


        final ObjectGraph objectGraph = ObjectGraph.create(new DaggerIBeanModule());

        long daggers = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                objectGraph.get(IBean.class).stuff();
            }
        }, threads).run(duration, primingRuns, "dagger plain prototype");

        final MutablePicoContainer picoContainer = new DefaultPicoContainer()
                .addComponent(IBean.class, Bean.class)
                .addComponent(IBean2.class, Bean2.class);

        long picos = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
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
            public void execute() {
                injector.getInstance(gKey).stuff();
            }
        }, threads).run(duration, primingRuns, "guice plain prototypes");

        final ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-plain-prototype.xml");

        long springs = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                applicationContext.getBean(Bean.class).stuff();
            }
        }, threads).run(duration, primingRuns, "spring plain prototypes");

        final se.jbee.inject.Injector silk = Bootstrap.injector(PrototypeSilkBeans.class);
        final Dependency<IBean> iBeanDependency = Dependency.dependency(IBean.class);

        long silks = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
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
            public void execute() {
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
            public void execute() {
                container2.get(key).yo();
            }
        }, threads).run(duration, primingRuns, "my singleton");

        final ObjectGraph objectGraph = ObjectGraph.create(new DaggerISingletonModule());

        long daggers = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                objectGraph.get(ISingleton.class).yo();
            }
        }, threads).run(duration, primingRuns, "dagger singleton");


        final PicoContainer picoContainer3 = new DefaultPicoContainer(new Caching())
                .addComponent(ISingleton.class, Singleton.class)
                .addComponent(ISingleton2.class, Singleton2.class);

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
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
            public void execute() {
                picoContainer3.getComponent(ISingleton.class).yo();
            }
        }, threads).run(duration, primingRuns, "pico singleton");




        final Injector injector3 = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ISingleton.class).to(Singleton.class).in(com.google.inject.Scopes.SINGLETON);
                bind(ISingleton2.class).to(Singleton2.class).in(com.google.inject.Scopes.SINGLETON);
            }
        });

        new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
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
            public void execute() {
                injector3.getInstance(ISingleton.class).yo();
            }
        }, threads).run(duration, primingRuns, "guice singleton");

        final ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-singleton.xml");

        long springs = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
                applicationContext.getBean(Singleton.class).yo();
            }
        }, threads).run(duration, primingRuns, "spring singleton");

        final se.jbee.inject.Injector silk = Bootstrap.injector(SingletonSilkBeans.class);
        final Dependency<ISingleton> iSingletonDependency = Dependency.dependency(ISingleton.class);

        long silks = new ConcurrentExecutionAssistant.TestThreadGroup(new ConcurrentExecutionAssistant.Execution() {
            @Override
            public void execute() {
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
            public void execute() {
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
            public void execute() {
                injector2.getInstance(ICyclicRef2.class);
            }
        }, threads).run(duration, primingRuns, "guice cyclic refs");

        printComparison(mines, guices, "guice");

    }
}
