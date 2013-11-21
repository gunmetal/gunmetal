package io.gunmetal.internal;

import io.gunmetal.AccessRestrictions;
import io.gunmetal.Dependency;
import io.gunmetal.Module;
import io.gunmetal.Qualifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.gunmetal.AccessRestrictions.Modules;

/**
 * @author rees.byars
 */
@RunWith(JUnit4.class)
public class ReflectiveModuleBinderTest {

    InternalProvider internalProvider = new InternalProvider() {
        @Override
        public <T> ProvisionStrategy<T> getProvisionStrategy(DependencyRequest dependencyRequest) {
            return null;
        }

        @Override
        public void register(Callback callback, BuildPhase phase) {
        }
    };

    Binder binder = new Binder() {
        @Override
        public <T> ComponentAdapter<T> bind(Dependency<?> dependency, ComponentAdapter<T> componentAdapter) {
            return null;
        }
    };

    ModuleBinder moduleBinder;


    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    public @interface Q {
    }


    @Q
    @Module(
            dependsOn = ReflectiveModuleBinder.class,
            onlyAccessibleFrom = TestModule.WhiteList.class
    )
    static class TestModule {

        @Modules(ReflectiveModuleBinder.class)
        class WhiteList implements AccessRestrictions.OnlyAccessibleFrom { }

        @Modules(ReflectiveModuleBinder.class)
        class BlackList implements AccessRestrictions.NotAccessibleFrom { }

        private static TestModule module() {
            return new TestModule();
        }
    }

    @Test
    public void testBind() {

        moduleBinder.bind(TestModule.class, internalProvider, binder);

    }

}
