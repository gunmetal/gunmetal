package io.gunmetal.builder;

import io.gunmetal.Component;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
class DefaultComponentAdapterFactory implements ComponentAdapterFactory {

    private final Injector.Factory injectorFactory;

    DefaultComponentAdapterFactory(Injector.Factory injectorFactory) {
        this.injectorFactory = injectorFactory;
    }

    @Override
    public <T> ComponentAdapter<T> create(Component component,
                                          AccessFilter<DependencyRequest> accessFilter,
                                          InternalProvider internalProvider) {
        return null;
    }

    @Override
    public <T> ComponentAdapter<T> create(Method providerMethod,
                                          AccessFilter<DependencyRequest> accessFilter,
                                          InternalProvider internalProvider) {

        // TODO two types of instantiators - constructor and method

        internalProvider.register(new InternalProvider.Callback() {
            @Override
            public void call() {
            }
        }, InternalProvider.BuildPhase.POST_WIRING);

        //component adapter

        Injector.StaticMethod injector = injectorFactory.create(providerMethod, internalProvider);
        return null;
    }

}
