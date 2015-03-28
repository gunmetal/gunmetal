package io.gunmetal.sandbox;

import com.google.common.eventbus.EventBus;
import io.gunmetal.Inject;
import io.gunmetal.ObjectGraph;
import io.gunmetal.Overrides;
import io.gunmetal.sandbox.testmocks.dongle.bl.Dongler;
import io.gunmetal.sandbox.testmocks.dongle.config.RootModule;
import io.gunmetal.sandbox.testmocks.dongle.scope.Scopes;
import io.gunmetal.sandbox.testmocks.dongle.ui.UiModule;
import io.gunmetal.sandbox.testmocks.dongle.ui.UserModule;
import io.gunmetal.sandbox.testmocks.dongle.ws.WsModule;
import io.gunmetal.spi.ProvisionStrategy;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author rees.byars
 */
@Overrides(allowFieldInjection = true, allowImplicitModuleDependency = true, allowFuzzyScopes = true)
public class ObjectGraphTest {

    @Inject EventBus eventBus;

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
                        .inject(this));

        assertNotNull(
                configGraph
                        .plus()
                        .buildTemplate(WsModule.class)
                        .newInstance()
                        .inject(this));

        ObjectGraph graph = configGraph
                .plus()
                .buildTemplate(UiModule.class, WsModule.class)
                .newInstance(new UserModule("test"));

        graph.inject(this);
        eventBus.post(new Dongler());
    }

}
