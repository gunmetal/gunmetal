package io.gunmetal.testmocks.dongle.config;

import com.google.common.eventbus.EventBus;
import io.gunmetal.Module;
import io.gunmetal.Provides;
import io.gunmetal.Singleton;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ResourceMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.testmocks.dongle.bl.Dongler;

import java.util.Collections;
import java.util.List;

/**
 * @author rees.byars
 */
@Module
public interface RootModule {

    @Provides @Singleton static EventBus eventBus() {
        return new EventBus();
    }

    @Provides static List<? extends ProvisionStrategyDecorator> decorators(EventBus eventBus) {
        return Collections.singletonList(new ProvisionStrategyDecorator() {
            @Override public <T> ProvisionStrategy<T> decorate(
                    ResourceMetadata<?> resourceMetadata, ProvisionStrategy<T> delegateStrategy, Linkers linkers) {

                System.out.println("building " + resourceMetadata);

                return (internalProvider, resolutionContext) -> {
                    EventBus e = eventBus;
                    System.out.println("visiting access of " + resourceMetadata);
                    T t = delegateStrategy.get(internalProvider, resolutionContext);
                    if (t instanceof Dongler)
                        e.register(t);
                    System.out.println("got " + t);

                    return t;
                };
            }
        });
    }

}
