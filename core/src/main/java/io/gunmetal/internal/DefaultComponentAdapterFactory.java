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
                baseProvisionStrategy(componentMetadata, instantiator, postInjector),
                internalProvider);

        return componentAdapter(componentMetadata, provisionStrategy, accessFilter);
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
                    if (e.metadata.equals(componentMetadata)) {
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

    private <T> ComponentAdapter<T> componentAdapter(final ComponentMetadata metadata,
                                                     final ProvisionStrategy<T> provisionStrategy,
                                                     final AccessFilter<DependencyRequest> accessFilter) {
        return new ComponentAdapter<T>() {
            @Override public ComponentMetadata metadata() {
                return metadata;
            }
            @Override public boolean isAccessibleTo(DependencyRequest dependencyRequest) {
                return accessFilter.isAccessibleTo(dependencyRequest);
            }
            @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                return provisionStrategy.get(internalProvider, resolutionContext);
            }
        };
    }
    
    private class CircularReferenceException extends RuntimeException {
        
        private ComponentMetadata metadata;
        private ProvisionStrategy<?> reverseStrategy;

        protected CircularReferenceException(String message) {
            super(message);
        }

        protected CircularReferenceException(ComponentMetadata metadata) {
            this.metadata = metadata;
        }

        public ComponentMetadata metadata() {
            return metadata;
        }

        public void setReverseStrategy(ProvisionStrategy<?> reverseStrategy) {
            this.reverseStrategy = reverseStrategy;
        }

        public ProvisionStrategy<?> getReverseStrategy() {
            return reverseStrategy;
        }

        @Override
        public String getMessage() {
            if (reverseStrategy != null) {
                return super.getMessage() + " of with metadata [" + metadata() + "]";
            }
            return super.getMessage();
        }
        
    }

}
