package com.github.overengineer.container.proxy.aop;

import com.github.overengineer.container.ComponentStrategyFactory;
import com.github.overengineer.container.module.BaseModule;
import com.github.overengineer.container.key.Generic;
import com.github.overengineer.container.proxy.JdkProxyHandlerFactory;
import com.github.overengineer.container.proxy.ProxyComponentStrategyFactory;
import com.github.overengineer.container.proxy.ProxyHandlerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rees.byars
 */
public class AopModule extends BaseModule {

    @Override
    protected void configure() {
        use(JdkProxyHandlerFactory.class).forType(ProxyHandlerFactory.class);
        use(ProxyComponentStrategyFactory.class).forType(ComponentStrategyFactory.class);
        use(JdkAopProxyHandlerFactory.class).forType(ProxyHandlerFactory.class);
        use(AdvisedInvocationFactory.class).forType(JoinPointInvocationFactory.class);
        use(DefaultPointcutInterpreter.class).forType(PointcutInterpreter.class);
        use(DefaultAopContainer.class).forType(AopContainer.class);
        use(new ArrayList<Aspect>()).forType(new Generic<List<Aspect>>() { });
    }
}
