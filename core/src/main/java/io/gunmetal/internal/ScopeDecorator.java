package io.gunmetal.internal;

import io.gunmetal.Provider;
import io.gunmetal.ProviderDecorator;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * @author rees.byars
 */
class ScopeDecorator implements ProvisionStrategyDecorator {

    private final Class<? extends Annotation> scopeAnnotationClass;

    ScopeDecorator(Class<? extends Annotation> scopeAnnotationClass) {
        this.scopeAnnotationClass = scopeAnnotationClass;
    }

    @Override
    public <T> ProvisionStrategy<T> decorate(final AnnotatedElement annotatedElement,
                                             final ComponentMetadata componentMetadata,
                                             final ProvisionStrategy<T> delegateStrategy) {
        return new ProvisionStrategy<T>() {
            @Override
            public T get(final InternalProvider internalProvider, final ResolutionContext resolutionContext) {
                return decorator(Metadata.scope(annotatedElement, scopeAnnotationClass))
                        .decorate(componentMetadata, new Provider<T>() {
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
