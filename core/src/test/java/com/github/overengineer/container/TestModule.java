package com.github.overengineer.container;

import com.github.overengineer.container.module.BaseModule;

/**
 */
public class TestModule extends BaseModule {

    @Override
    public void configure() {

        use(Bean.class).forType(IBean.class);


    }

}
