package io.gunmetal.internal;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
class DefaultProvisionStrategyFactory implements ProvisionStrategyFactory {

    private final Injectors.Factory injectorFactory;
    private final ProvisionStrategyDecorator strategyDecorator;

    DefaultProvisionStrategyFactory(Injectors.Factory injectorFactory, MetadataAdapter metadataAdapter,
                                    ProvisionStrategyDecorator strategyDecorator) {
        this.injectorFactory = injectorFactory;
        this.strategyDecorator = strategyDecorator;
    }

    @Override
    public <T, P extends AnnotatedElement> ProvisionStrategy<T> create(
            final ComponentMetadata<P> componentMetadata,
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

        return strategyDecorator.decorate(
                componentMetadata,
                baseProvisionStrategy(componentMetadata, instantiator, postInjector),
                internalProvider);
    }

    private <T> ProvisionStrategy<T> baseProvisionStrategy(final ComponentMetadata componentMetadata,
                                                     final Instantiator<T> instantiator,
                                                     final Injectors.Injector<T> injector) {
        return new ProvisionStrategy<T>() {
            @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                ResolutionContext.ProvisionContext<T> strategyContext = resolutionContext.getProvisionContext(this);
                if (strategyContext.state != ResolutionContext.States.NEW) {
                    if (strategyContext.state == ResolutionContext.States.PRE_INJECTION) {
                        return strategyContext.component;
                    }
                    throw new CircularReferenceException(componentMetadata);
                }
                strategyContext.state = ResolutionContext.States.PRE_INSTANTIATION;
                try {
                    strategyContext.component = instantiator.getInstance(internalProvider, resolutionContext);
                    strategyContext.state = ResolutionContext.States.PRE_INJECTION;
                    injector.inject(strategyContext.component, internalProvider, resolutionContext);
                    strategyContext.state = ResolutionContext.States.NEW;
                    return strategyContext.component;
                } catch (CircularReferenceException e) {
                    strategyContext.state = ResolutionContext.States.NEW;
                    if (e.metadata().equals(componentMetadata)) {
                        e.getReverseStrategy().get(internalProvider, resolutionContext);
                        return strategyContext.component;
                    } else if (e.getReverseStrategy() == null) {
                        e.setReverseStrategy(this);
                    }
                    throw e;
                }
            }

        };
    }

}
