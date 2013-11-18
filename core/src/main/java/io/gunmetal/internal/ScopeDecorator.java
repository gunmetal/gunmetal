package io.gunmetal.internal;

import io.gunmetal.Provider;
import io.gunmetal.ProviderDecorator;

import java.lang.annotation.Annotation;

/**
 * @author rees.byars
 */
class ScopeDecorator implements ProvisionStrategyDecorator {

    private final Class<? extends Annotation> scopeAnnotationClass;

    ScopeDecorator(Class<? extends Annotation> scopeAnnotationClass) {
        this.scopeAnnotationClass = scopeAnnotationClass;
    }

    @Override
    public <T> ProvisionStrategy<T> decorate(final ComponentMetadata componentMetadata,
                                             final ProvisionStrategy<T> delegateStrategy) {
        return new ProvisionStrategy<T>() {

            ProviderDecorator providerDecorator =
                    decorator(Metadata.scope(componentMetadata.provider(), scopeAnnotationClass));

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

    private ProviderDecorator decorator(Annotation scopeAnnotation) {
        return null;
    }
}
