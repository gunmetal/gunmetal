package io.gunmetal.spi.impl;

import io.gunmetal.spi.InjectionResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * @author rees.byars
 */
public final class AnnotationInjectionResolver implements InjectionResolver {

    private final Class<? extends Annotation> annotationType;

    public AnnotationInjectionResolver(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    @Override public boolean shouldInject(AnnotatedElement element) {
        return element.isAnnotationPresent(annotationType);
    }

}
