package io.gunmetal.internal;

import io.gunmetal.AccessRestrictions;
import io.gunmetal.Component;
import io.gunmetal.Dependency;
import io.gunmetal.Module;
import io.gunmetal.Qualifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static io.gunmetal.AccessRestrictions.Modules;

/**
 * @author rees.byars
 */
@RunWith(JUnit4.class)
public class ReflectiveModuleBinderTest {

    ComponentAdapterFactory componentAdapterFactory = new ComponentAdapterFactory() {

        @Override
        public <T> ComponentAdapter<T> create(Component component, AccessFilter<DependencyRequest> accessFilter, InternalProvider internalProvider) {
            return null;
        }

        @Override
        public <T> ComponentAdapter<T> create(Method providerMethod, AccessFilter<DependencyRequest> accessFilter, InternalProvider internalProvider) {
            return null;
        }

    };

    MetadataAdapter metadataAdapter = new MetadataAdapter() {
        @Override
        public Class<? extends Annotation> getQualifierAnnotation() {
            return Qualifier.class;
        }

        @Override
        public Class<? extends Annotation> getScopeAnnotation() {
            return null;
        }
    };

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

    ModuleBinder moduleBinder = new ReflectiveModuleBinder(componentAdapterFactory, metadataAdapter);


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
