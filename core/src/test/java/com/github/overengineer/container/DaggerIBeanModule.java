package com.github.overengineer.container;

import dagger.Module;
import dagger.Provides;

@Module(library = true, injects = IBean.class)
public class DaggerIBeanModule {

    @Provides IBean provideIBean(IBean2 bean2) {
        return new Bean(bean2);
    }

    @Provides IBean2 provideIBean2() {
        return new Bean2();
    }

}
