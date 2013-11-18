package io.gunmetal.internal;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
class DefaultComponentAdapterFactory implements ComponentAdapterFactory {

    private final Injectors.Factory injectorFactory;
    private final ProvisionStrategyDecorator strategyDecorator;

    DefaultComponentAdapterFactory(Injectors.Factory injectorFactory, MetadataAdapter metadataAdapter,
                                   ProvisionStrategyDecorator strategyDecorator) {
        this.injectorFactory = injectorFactory;
        this.strategyDecorator = strategyDecorator;
    }

    @Override
    public <T> ComponentAdapter<T> create(final ComponentMetadata componentMetadata,
                                          AccessFilter<DependencyRequest> accessFilter,
                                          InternalProvider internalProvider) {



        Injectors.StaticInjector providerInjector =
                injectorFactory.staticInjector((Method) componentMetadata.provider(), componentMetadata, internalProvider);

        internalProvider.register(new Callback() {
            @Override
            public void call() {
            }
        }, InternalProvider.BuildPhase.POST_WIRING);

        //component adapter


        Instantiator<T> instantiator = null;

        Injectors.Injector<T> postInjector = null;

        ProvisionStrategy<T> provisionStrategy = strategyDecorator.decorate(
                componentMetadata,
                baseProvisionStrategy(componentMetadata, instantiator, postInjector));

        return null;
    }

    private <T> ProvisionStrategy<T> baseProvisionStrategy(final ComponentMetadata componentMetadata,
                                                     final Instantiator<T> instantiator,
                                                     final Injectors.Injector<T> injector) {
        return new ProvisionStrategy<T>() {

            @Override
            public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                return null;
            }

        };
    }

    private <T> ComponentAdapter<T> componentAdapter() {
        return null;
    }

}
