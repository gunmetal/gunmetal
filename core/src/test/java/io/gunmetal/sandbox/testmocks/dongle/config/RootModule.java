package io.gunmetal.sandbox.testmocks.dongle.config;

import com.google.common.eventbus.EventBus;
import io.gunmetal.Module;
import io.gunmetal.MultiBind;
import io.gunmetal.Provides;
import io.gunmetal.Singleton;
import io.gunmetal.sandbox.testmocks.dongle.bl.Dongler;
import io.gunmetal.spi.ProvisionStrategyDecorator;

/**
 * @author rees.byars
 */
@Module
public interface RootModule {

    @Provides @Singleton static EventBus eventBus() {
        return new EventBus();
    }

    @Provides @MultiBind static ProvisionStrategyDecorator eventDecorator(EventBus eventBus) {
        return (resourceMetadata, delegateStrategy, linkers) -> {

            System.out.println("building " + resourceMetadata);

            return (internalProvider, resolutionContext) -> {
                System.out.println("visiting access of " + resourceMetadata);
                Object t = delegateStrategy.get(internalProvider, resolutionContext);
                if (t instanceof Dongler)
                    eventBus.register(t);
                System.out.println("got " + t);

                return t;
            };
        };
    }

}
