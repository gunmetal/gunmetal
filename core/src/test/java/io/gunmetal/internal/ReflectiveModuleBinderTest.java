package io.gunmetal.internal;

import io.gunmetal.Module;
import io.gunmetal.Qualifier;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author rees.byars
 */
@Ignore
@RunWith(JUnit4.class)
public class ReflectiveModuleBinderTest {

    InternalProvider internalProvider = new InternalProvider() {
        @Override
        public <T> ProvisionStrategy<T> getProvisionStrategy(DependencyRequest dependencyRequest) {
            return null;
        }
    };

    ModuleParser moduleParser;


    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    public @interface Q {
    }


    @Q
    @Module(
            dependsOn = ModuleParserImpl.class,
            onlyAccessibleFrom = TestModule.WhiteList.class
    )
    static class TestModule {

        @io.gunmetal.WhiteList.Modules(ModuleParserImpl.class)
        class WhiteList implements io.gunmetal.WhiteList { }
        @io.gunmetal.BlackList.Modules(ModuleParserImpl.class)
        class BlackList implements io.gunmetal.BlackList { }

        private static TestModule module() {
            return new TestModule();
        }
    }

}
