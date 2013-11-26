package io.gunmetal.internal;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
class DefaultProvisionStrategyFactory implements ProvisionStrategyFactory {

    private final Injectors.Factory injectorFactory;
    private final ProvisionStrategyDecorator strategyDecorator;

    DefaultProvisionStrategyFactory(Injectors.Factory injectorFactory,
                                    ProvisionStrategyDecorator strategyDecorator) {
        this.injectorFactory = injectorFactory;
        this.strategyDecorator = strategyDecorator;
    }

    @Override
    public <T, P extends AnnotatedElement> ProvisionStrategy<T> create(
            final ComponentMetadata<P> componentMetadata,
            InternalProvider internalProvider) {

        Injectors.Instantiator<T> instantiator;
        Injectors.Injector<T> postInjector;

        if (componentMetadata.providerKind() == ProviderKind.CLASS) {
            instantiator = injectorFactory.instantiator(
                    (Class<?>) componentMetadata.provider(), componentMetadata, internalProvider);
            postInjector = injectorFactory.composite(componentMetadata, internalProvider);
        } else if (componentMetadata.providerKind() == ProviderKind.METHOD) {
            instantiator = injectorFactory.instantiator(
                    (Method) componentMetadata.provider(), componentMetadata, internalProvider);
            postInjector = injectorFactory.lazy(componentMetadata);
        } else {
            throw new UnsupportedOperationException("The ProviderKind ["
                    + componentMetadata.providerKind() + "] is not yet supported");
        }

        return strategyDecorator.decorate(
                componentMetadata,
                baseProvisionStrategy(componentMetadata, instantiator, postInjector),
                internalProvider);
    }

    private <T> ProvisionStrategy<T> baseProvisionStrategy(final ComponentMetadata componentMetadata,
                                                     final Injectors.Instantiator<T> instantiator,
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
