package io.gunmetal.testmocks.dongle.config;

import io.gunmetal.Module;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Scope;
import io.gunmetal.testmocks.dongle.scope.Scopes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author rees.byars
 */
@Module
public class RootModule {

    static List<? extends ProvisionStrategyDecorator> decorators() {
        return Collections.emptyList();
    }

    static Map<? extends Scope, ? extends ProvisionStrategyDecorator> scopeDecorators() {
        // warning - this scope could lead to memory leaks as it never removes the thread local data
        return Collections.singletonMap(
                Scopes.THREAD,
                new ProvisionStrategyDecorator() {
                    @Override public <T> ProvisionStrategy<T> decorate(
                            ComponentMetadata<?> componentMetadata,
                            final ProvisionStrategy<T> delegateStrategy,
                            Linkers linkers) {
                        final ThreadLocal<T> threadLocal = new ThreadLocal<>();
                        return (p, c) -> {
                            T t = threadLocal.get();
                            if (t == null) {
                                t = delegateStrategy.get(p, c);
                                threadLocal.set(t);
                            }
                            return t;
                        };
                    }
                });
    }

}
