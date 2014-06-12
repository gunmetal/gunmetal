/*
 * Copyright (c) 2013.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gunmetal.internal;

import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.spi.Scope;
import io.gunmetal.spi.ScopeBindings;
import io.gunmetal.spi.Scopes;

/**
 * @author rees.byars
 */
class ScopeDecorator implements ProvisionStrategyDecorator {

    private final ScopeBindings scopeBindings;

    ScopeDecorator(ScopeBindings scopeBindings) {
        this.scopeBindings = scopeBindings;
    }

    @Override
    public <T> ProvisionStrategy<T> decorate(
            final ComponentMetadata<?> componentMetadata,
            final ProvisionStrategy<T> delegateStrategy,
            final Linkers linkers) {

        final Scope scope = componentMetadata.scope();

        if (scope == Scopes.PROTOTYPE) {
            return delegateStrategy;
        }

        if (scope == Scopes.SINGLETON && componentMetadata.eager()) {
            return new ProvisionStrategy<T>() {
                T singleton;
                { linkers.addEagerLinker(this::get); }
                @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                    return singleton;
                }
            };
        }

        if (scope == Scopes.SINGLETON) {
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

        return scopeBindings.decoratorFor(scope).decorate(componentMetadata, delegateStrategy, linkers);

    }

}