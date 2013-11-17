package io.gunmetal.internal;

import io.gunmetal.Component;
import io.gunmetal.CompositeQualifier;

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
class DefaultComponentAdapterFactory implements ComponentAdapterFactory {

    private final Injectors.Factory injectorFactory;
    private final MetadataAdapter metadataAdapter;
    private final ProvisionStrategyDecorator strategyDecorator;

    DefaultComponentAdapterFactory(Injectors.Factory injectorFactory, MetadataAdapter metadataAdapter,
                                   ProvisionStrategyDecorator strategyDecorator) {
        this.injectorFactory = injectorFactory;
        this.metadataAdapter = metadataAdapter;
        this.strategyDecorator = strategyDecorator;
    }

    @Override
    public <T> ComponentAdapter<T> create(Component component, ModuleAdapter moduleAdapter,
                                          AccessFilter<DependencyRequest> accessFilter,
                                          InternalProvider internalProvider) {
        return null;
    }

    @Override
    public <T> ComponentAdapter<T> create(final Method providerMethod, final ModuleAdapter moduleAdapter,
                                          AccessFilter<DependencyRequest> accessFilter,
                                          InternalProvider internalProvider) {

        final CompositeQualifier qualifier = Metadata.qualifier(providerMethod, moduleAdapter,
                metadataAdapter.qualifierAnnotation());

        ComponentMetadata componentMetadata = new ComponentMetadata() {

            @Override public Object origin() {
                return providerMethod;
            }

            @Override public Class<?> originClass() {
                return providerMethod.getDeclaringClass();
            }

            @Override public ModuleAdapter moduleAdapter() {
                return moduleAdapter;
            }

            @Override public CompositeQualifier compositeQualifier() {
                return qualifier;
            }

        };

        Injectors.StaticInjector providerInjector =
                injectorFactory.staticInjector(providerMethod, componentMetadata, internalProvider);

        internalProvider.register(new Callback() {
            @Override
            public void call() {
            }
        }, InternalProvider.BuildPhase.POST_WIRING);

        //component adapter


        Instantiator<T> instantiator = null;

        Injectors.Injector<T> postInjector = null;

        ProvisionStrategy<T> provisionStrategy = strategyDecorator.decorate(
                providerMethod,
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
