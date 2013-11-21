package io.gunmetal.internal;

import io.gunmetal.ProviderDecorator;

/**
 * @author rees.byars
 */
interface ScopeBindings {
    ProviderDecorator decorator(Scope scope);
}
