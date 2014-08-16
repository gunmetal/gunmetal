package io.gunmetal;

import io.gunmetal.spi.ProvisionMetadata;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.testmocks.dongle.config.RootModule;
import io.gunmetal.testmocks.dongle.layers.Ui;
import io.gunmetal.testmocks.dongle.layers.Ws;
import io.gunmetal.testmocks.dongle.scope.Scopes;
import io.gunmetal.testmocks.dongle.ui.DongleController;
import io.gunmetal.testmocks.dongle.ui.UiModule;
import io.gunmetal.testmocks.dongle.ui.UserModule;
import io.gunmetal.testmocks.dongle.ws.DongleResource;
import io.gunmetal.testmocks.dongle.ws.WsModule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author rees.byars
 */
public class ObjectGraphTest {

    @Ui class ControllerDependency implements Dependency<DongleController> { }
    @Ws class ResourceDependency implements Dependency<DongleResource> { }

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
                        io.gunmetal.testmocks.dongle.scope.Thread.class,
                        Scopes.THREAD,
                        new ProvisionStrategyDecorator() {
                            @Override public <T> ProvisionStrategy<T> decorate(
                                    ProvisionMetadata<?> componentMetadata,
                                    final ProvisionStrategy<T> delegateStrategy,
                                    Linkers linkers) {
                                final ThreadLocal<T> threadLocal = new ThreadLocal<>();
                                ProvisionStrategy<T> provisionStrategy = (p, c) -> {
                                    T t = threadLocal.get();
                                    if (t == null) {
                                        t = delegateStrategy.get(p, c);
                                        threadLocal.set(t);
                                    }
                                    return t;
                                };
                                { if (componentMetadata.eager()) linkers.addEagerLinker(provisionStrategy::get); }
                                return provisionStrategy;
                            }
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
    }

}
