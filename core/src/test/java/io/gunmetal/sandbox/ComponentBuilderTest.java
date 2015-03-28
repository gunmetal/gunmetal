package io.gunmetal.sandbox;

import com.google.common.eventbus.EventBus;
import io.gunmetal.Inject;
import io.gunmetal.Component;
import io.gunmetal.Module;
import io.gunmetal.Overrides;
import io.gunmetal.internal.ComponentBuilder;
import io.gunmetal.sandbox.testmocks.dongle.bl.Dongler;
import io.gunmetal.sandbox.testmocks.dongle.config.RootModule;
import io.gunmetal.sandbox.testmocks.dongle.scope.Scopes;
import io.gunmetal.sandbox.testmocks.dongle.ui.UiModule;
import io.gunmetal.sandbox.testmocks.dongle.ui.UserModule;
import io.gunmetal.sandbox.testmocks.dongle.ws.WsModule;
import io.gunmetal.spi.ProvisionStrategy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author rees.byars
 */
@Overrides(allowFieldInjection = true, allowImplicitModuleDependency = true, allowFuzzyScopes = true)
public class ComponentBuilderTest {

    @Inject EventBus eventBus;

    @Module(dependsOn = RootModule.class)
    public interface RootComponent {

        ComponentBuilder plus();

        public interface Factory {

            RootComponent create();

        }

    }

    @Module(dependsOn = {UiModule.class, WsModule.class})
    public interface MainComponent {

        void inject(ComponentBuilderTest test);

        EventBus eventBus();

        public interface Factory {

            MainComponent create(UserModule userModule);

        }

    }

    @Test
    public void testBasic() {

        RootComponent rootComponent = Component
                .builder()
                .requireAcyclic()
                .requireExplicitModuleDependencies()
                .restrictPluralQualifiers()
                .restrictSetterInjection()
                .restrictFieldInjection()
                .addScope(
                        io.gunmetal.sandbox.testmocks.dongle.scope.Thread.class,
                        Scopes.THREAD,
                        (componentMetadata, delegateStrategy, linkers) -> {
                            final ThreadLocal<Object> threadLocal = new ThreadLocal<>();
                            ProvisionStrategy provisionStrategy = (p, c) -> {
                                Object t = threadLocal.get();
                                if (t == null) {
                                    t = delegateStrategy.get(p, c);
                                    threadLocal.set(t);
                                }
                                return t;
                            };
                            {
                                if (componentMetadata.eager()) linkers.addEagerLinker(provisionStrategy::get);
                            }
                            return provisionStrategy;
                        })
                .build(RootComponent.Factory.class)
                .create();

        assertNotNull(
                rootComponent
                        .plus()
                        .build(MainComponent.Factory.class)
                        .create(new UserModule("test")));


        MainComponent graph = rootComponent
                .plus()
                .build(MainComponent.Factory.class)
                .create(new UserModule("test"));

        graph.inject(this);
        eventBus.post(new Dongler());

        assertEquals(eventBus, graph.eventBus());
    }

}
