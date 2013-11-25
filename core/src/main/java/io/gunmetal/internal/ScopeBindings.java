package io.gunmetal.internal;

import io.gunmetal.ProviderDecorator;

/**
 * @author rees.byars
 */
interface ScopeBindings {
    ProviderDecorator decoratorFor(Scope scope);
}
