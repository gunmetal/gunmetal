package io.gunmetal.internal;

import io.gunmetal.Provider;
import io.gunmetal.ProviderDecorator;

/**
 * @author rees.byars
 */
class ScopeDecorator implements ProvisionStrategyDecorator {

    private final AnnotationResolver<Scope> scopeResolver;
    private final ScopeBindings scopeBindings;
    private final Linkers linkers;

    ScopeDecorator(AnnotationResolver<Scope> scopeResolver, ScopeBindings scopeBindings, Linkers linkers) {
        this.scopeResolver = scopeResolver;
        this.scopeBindings = scopeBindings;
        this.linkers = linkers;
    }

    @Override
    public <T> ProvisionStrategy<T> decorate(
            final ComponentMetadata<?> componentMetadata,
            final ProvisionStrategy<T> delegateStrategy) {

        final Scope scope = scopeResolver.resolve(componentMetadata.provider());

        if (scope == Scopes.PROTOTYPE) {
            return delegateStrategy;
        }

        if (scope == Scopes.EAGER_SINGLETON) {
            return new ProvisionStrategy<T>() {
                T singleton;
                {
                    linkers.add(new Linker() {
                        @Override public void link(InternalProvider internalProvider, ResolutionContext linkingContext) {
                            singleton = delegateStrategy.get(internalProvider, linkingContext);
                        }
                    }, LinkingPhase.EAGER_INSTANTIATION);
                }
                @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                    return singleton;
                }
            };
        }

        if (scope == Scopes.LAZY_SINGLETON) {
            return new ProvisionStrategy<T>() {
                volatile T singleton;
                @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                    if (singleton == null) {
                        synchronized (this) {
                            if (singleton == null) {
                                singleton = delegateStrategy.get(internalProvider, resolutionContext);
                            }
                        }
                    }
                    return singleton;
                }
            };
        }

        return new ProvisionStrategy<T>() {
            ProviderDecorator providerDecorator = scopeBindings.decoratorFor(scope);
            @Override
            public T get(final InternalProvider internalProvider, final ResolutionContext resolutionContext) {
                return providerDecorator.decorate(componentMetadata, new Provider<T>() {
                            @Override
                            public T get() {
                                return delegateStrategy.get(internalProvider, resolutionContext);
                            }
                        }).get();
            }
        };
    }

}
