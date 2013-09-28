package com.github.overengineer.gunmetal.proxy.aop;

import com.github.overengineer.gunmetal.ComponentStrategyFactory;
import com.github.overengineer.gunmetal.module.BaseModule;
import com.github.overengineer.gunmetal.key.Generic;
import com.github.overengineer.gunmetal.proxy.JdkProxyHandlerFactory;
import com.github.overengineer.gunmetal.proxy.ProxyComponentStrategyFactory;
import com.github.overengineer.gunmetal.proxy.ProxyHandlerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rees.byars
 */
public class AopModule extends BaseModule {

    private static final long serialVersionUID = -3574152504964488937L;

    @Override
    public void configure() {

        use(JdkProxyHandlerFactory.class)
                .forType(ProxyHandlerFactory.class);

        use(ProxyComponentStrategyFactory.class)
                .forType(ComponentStrategyFactory.class);

        use(JdkAopProxyHandlerFactory.class)
                .forType(ProxyHandlerFactory.class);

        use(AdvisedInvocationFactory.class)
                .forType(JoinPointInvocationFactory.class);

        use(DefaultPointcutInterpreter.class)
                .forType(PointcutInterpreter.class);

        use(DefaultAopContainer.class)
                .forType(AopContainer.class);

        use(new ArrayList<Aspect>())
                .forType(new Generic<List<Aspect>>() {

                    private static final long serialVersionUID = 1L;

                });

    }
}
