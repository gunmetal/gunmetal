package com.github.overengineer.gunmetal.proxy;

import com.github.overengineer.gunmetal.module.BaseModule;
import com.github.overengineer.gunmetal.ComponentStrategyFactory;
import com.github.overengineer.gunmetal.DefaultComponentStrategyFactory;

/**
 * @author rees.byars
 */
public class ProxyModule extends BaseModule {

    private static final long serialVersionUID = 3008168720237925540L;

    @Override
    public void configure() {

        use(JdkProxyHandlerFactory.class)
                .forType(ProxyHandlerFactory.class);

        use(DefaultComponentStrategyFactory.class)
                .forType(ComponentStrategyFactory.class);

        use(ProxyComponentStrategyFactory.class)
                .forType(ComponentStrategyFactory.class);

        use(DefaultHotSwappableContainer.class)
                .forType(HotSwappableContainer.class);

    }

}
