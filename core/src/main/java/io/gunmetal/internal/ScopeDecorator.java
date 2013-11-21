package io.gunmetal.internal;

import io.gunmetal.Provider;
import io.gunmetal.ProviderDecorator;

/**
 * @author rees.byars
 */
class ScopeDecorator implements ProvisionStrategyDecorator {

    private final ScopeResolver scopeResolver;
    private final ScopeBindings scopeBindings;

    ScopeDecorator(ScopeResolver scopeResolver, ScopeBindings scopeBindings) {
        this.scopeResolver = scopeResolver;
        this.scopeBindings = scopeBindings;
    }

    @Override
    public <T> ProvisionStrategy<T> decorate(final ComponentMetadata componentMetadata,
                                             final ProvisionStrategy<T> delegateStrategy,
                                             final InternalProvider internalProvider) {

        final Scope scope = scopeResolver.resolve(componentMetadata.provider());

        if (scope == Scopes.PROTOTYPE) {
            return delegateStrategy;
        }

        if (scope == Scopes.EAGER_SINGLETON) {
            return new ProvisionStrategy<T>() {
                T singleton;
                {
                    internalProvider.register(new Callback() {
                        @Override public void call() {
                            singleton = delegateStrategy.get(internalProvider, ResolutionContext.Factory.create());
                        }
                    }, InternalProvider.BuildPhase.EAGER_INSTANTIATION);
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
            ProviderDecorator providerDecorator = scopeBindings.decorator(scope);
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
