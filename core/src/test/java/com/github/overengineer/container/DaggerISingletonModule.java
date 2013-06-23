package com.github.overengineer.container;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module(library = true, injects = DefaultContainerTest.ISingleton.class)
public class DaggerISingletonModule {

    @Provides @Singleton DefaultContainerTest.ISingleton provideISingleton() {
        return new DefaultContainerTest.Singleton();
    }

}
