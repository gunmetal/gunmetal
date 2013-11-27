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

import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
class ProvisionStrategyFactoryImpl implements ProvisionStrategyFactory {

    private final Injectors.Factory injectorFactory;
    private final ProvisionStrategyDecorator strategyDecorator;

    ProvisionStrategyFactoryImpl(Injectors.Factory injectorFactory,
                                 ProvisionStrategyDecorator strategyDecorator) {
        this.injectorFactory = injectorFactory;
        this.strategyDecorator = strategyDecorator;
    }

    @Override public <T> ProvisionStrategy<T> withClassProvider(ComponentMetadata<Class> componentMetadata) {
        Injectors.Instantiator<T> instantiator =
                injectorFactory.constructorInstantiator(componentMetadata);
        Injectors.Injector<T> postInjector = injectorFactory.composite(componentMetadata);
        return strategyDecorator.decorate(
                componentMetadata,
                baseProvisionStrategy(componentMetadata, instantiator, postInjector));
    }

    @Override public <T> ProvisionStrategy<T> withMethodProvider(ComponentMetadata<Method> componentMetadata) {
        Injectors.Instantiator<T> instantiator =
                injectorFactory.methodInstantiator(componentMetadata);
        Injectors.Injector<T> postInjector = injectorFactory.lazy(componentMetadata);
        return strategyDecorator.decorate(
                componentMetadata,
                baseProvisionStrategy(componentMetadata, instantiator, postInjector));
    }

    private <T> ProvisionStrategy<T> baseProvisionStrategy(final ComponentMetadata componentMetadata,
                                                     final Injectors.Instantiator<T> instantiator,
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
                    if (e.metadata().equals(componentMetadata)) {
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

}
