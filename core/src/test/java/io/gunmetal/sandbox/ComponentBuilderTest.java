package io.gunmetal.sandbox;

import com.google.common.eventbus.EventBus;
import io.gunmetal.ComponentFactory;
import io.gunmetal.Inject;
import io.gunmetal.Module;
import io.gunmetal.MultiBind;
import io.gunmetal.Named;
import io.gunmetal.Overrides;
import io.gunmetal.internal.ComponentTemplate;
import io.gunmetal.sandbox.testmocks.dongle.bl.Dongler;
import io.gunmetal.sandbox.testmocks.dongle.config.RootModule;
import io.gunmetal.sandbox.testmocks.dongle.scope.Scopes;
import io.gunmetal.sandbox.testmocks.dongle.ui.UiModule;
import io.gunmetal.sandbox.testmocks.dongle.ui.UserModule;
import io.gunmetal.sandbox.testmocks.dongle.ws.WsModule;
import io.gunmetal.spi.GunmetalComponent;
import io.gunmetal.spi.Option;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author rees.byars
 */
@Overrides(allowFieldInjection = true, allowImplicitModuleDependency = true, allowFuzzyScopes = true)
public class ComponentBuilderTest {

    @Inject EventBus eventBus;

    @Module(component = true)
    public interface RootComponent {

        @MultiBind List<ProvisionStrategyDecorator> strategyDecorators();

        @Named("thread") ProvisionStrategyDecorator threadScope();

        @ComponentFactory(dependsOn = RootModule.class)
        public interface Factory {

            RootComponent create();

        }

    }

    @Module(component = true)
    public interface MainComponent {

        void inject(ComponentBuilderTest test);

        EventBus eventBus();

        @ComponentFactory(dependsOn = {UiModule.class, WsModule.class, RootModule.class})
        public interface Factory {

            MainComponent create(UserModule userModule, RootComponent rootComponent);

        }

    }

    @Test
    public void testBasic() {

        RootComponent rootComponent =
                ComponentTemplate.build(
                        new GunmetalComponent.Default(
                                Option.REQUIRE_ACYCLIC,
                                Option.REQUIRE_EXPLICIT_MODULE_DEPENDENCIES,
                                Option.RESTRICT_PLURAL_QUALIFIERS,
                                Option.RESTRICT_SETTER_INJECTION,
                                Option.RESTRICT_FIELD_INJECTION),
                        RootComponent.Factory.class)
                .create();

        GunmetalComponent gunmetalComponent = new GunmetalComponent.Default(
                Option.REQUIRE_ACYCLIC,
                Option.REQUIRE_EXPLICIT_MODULE_DEPENDENCIES,
                Option.RESTRICT_PLURAL_QUALIFIERS,
                Option.RESTRICT_SETTER_INJECTION,
                Option.RESTRICT_FIELD_INJECTION
        ).addScope(
                io.gunmetal.sandbox.testmocks.dongle.scope.Thread.class,
                Scopes.THREAD,
                rootComponent.threadScope());

        gunmetalComponent.strategyDecorators().addAll(rootComponent.strategyDecorators());
        MainComponent graph = ComponentTemplate.build(gunmetalComponent, MainComponent.Factory.class)
                .create(new UserModule("test"), rootComponent);

        graph.inject(this);
        eventBus.post(new Dongler());

        assertEquals(eventBus, graph.eventBus());
    }

}
