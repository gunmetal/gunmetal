package io.gunmetal.sandbox;

import com.google.common.eventbus.EventBus;
import io.gunmetal.Dependency;
import io.gunmetal.ObjectGraph;
import io.gunmetal.sandbox.testmocks.dongle.bl.Dongler;
import io.gunmetal.sandbox.testmocks.dongle.config.RootModule;
import io.gunmetal.sandbox.testmocks.dongle.layers.Ui;
import io.gunmetal.sandbox.testmocks.dongle.layers.Ws;
import io.gunmetal.sandbox.testmocks.dongle.scope.Scopes;
import io.gunmetal.sandbox.testmocks.dongle.ui.DongleController;
import io.gunmetal.sandbox.testmocks.dongle.ui.UiModule;
import io.gunmetal.sandbox.testmocks.dongle.ui.UserModule;
import io.gunmetal.sandbox.testmocks.dongle.ws.DongleResource;
import io.gunmetal.sandbox.testmocks.dongle.ws.WsModule;
import io.gunmetal.spi.ProvisionStrategy;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author rees.byars
 */
public class ObjectGraphTest {

    @Ui
    class ControllerDependency implements Dependency<DongleController> {
    }

    @Ws
    class ResourceDependency implements Dependency<DongleResource> {
    }

    @Test
    public void testBasic() {

        ObjectGraph configGraph = ObjectGraph
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
                .buildTemplate(RootModule.class)
                .newInstance();

        assertNotNull(
                configGraph
                        .plus()
                        .buildTemplate(UiModule.class, WsModule.class)
                        .newInstance(new UserModule("test"))
                        .get(ControllerDependency.class));

        assertNotNull(
                configGraph
                        .plus()
                        .buildTemplate(WsModule.class)
                        .newInstance()
                        .get(ResourceDependency.class));

        class EventBusDep implements Dependency<EventBus> {
        }

        ObjectGraph graph = configGraph
                .plus()
                .buildTemplate(UiModule.class, WsModule.class)
                .newInstance(new UserModule("test"));

        graph.get(ControllerDependency.class);
        EventBus eventBus = graph.get(EventBusDep.class);
        eventBus.post(new Dongler());
    }

}
