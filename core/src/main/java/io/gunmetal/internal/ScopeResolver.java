package io.gunmetal.internal;

import java.lang.reflect.AnnotatedElement;

/**
 * @author rees.byars
 */
interface ScopeResolver {
    Scope resolve(AnnotatedElement annotatedElement);
}
