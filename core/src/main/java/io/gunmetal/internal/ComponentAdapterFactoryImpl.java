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
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.ResolutionContext;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
class ComponentAdapterFactoryImpl implements ComponentAdapterFactory {

    private final InjectorFactory injectorFactory;
    private final ProvisionStrategyDecorator strategyDecorator;

    ComponentAdapterFactoryImpl(InjectorFactory injectorFactory,
                                ProvisionStrategyDecorator strategyDecorator) {
        this.injectorFactory = injectorFactory;
        this.strategyDecorator = strategyDecorator;
    }

    @Override public <T> ComponentAdapter<T> withClassProvider(ComponentMetadata<Class<?>> componentMetadata) {
        Instantiator<T> instantiator =
                injectorFactory.constructorInstantiator(componentMetadata);
        Injector<T> postInjector = injectorFactory.compositeInjector(componentMetadata);
        ProvisionStrategy<T> provisionStrategy = strategyDecorator.decorate(
                componentMetadata,
                baseProvisionStrategy(componentMetadata, instantiator, postInjector));
        return componentAdapter(
                componentMetadata,
                provisionStrategy,
                instantiator,
                postInjector);
    }

    @Override public <T> ComponentAdapter<T> withMethodProvider(ComponentMetadata<Method> componentMetadata) {
        Instantiator<T> instantiator =
                injectorFactory.methodInstantiator(componentMetadata);
        Injector<T> postInjector = injectorFactory.lazyCompositeInjector(componentMetadata);
        ProvisionStrategy<T> provisionStrategy = strategyDecorator.decorate(
                componentMetadata,
                baseProvisionStrategy(componentMetadata, instantiator, postInjector));
        return componentAdapter(
                componentMetadata,
                provisionStrategy,
                instantiator,
                postInjector);
    }

    private <T> ProvisionStrategy<T> baseProvisionStrategy(final ComponentMetadata<?> componentMetadata,
                                                     final Instantiator<T> instantiator,
                                                     final Injector<T> injector) {
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
                    strategyContext.component = instantiator.newInstance(internalProvider, resolutionContext);
                    strategyContext.state = ResolutionContext.States.PRE_INJECTION;
                    injector.inject(strategyContext.component, internalProvider, resolutionContext);
                    strategyContext.state = ResolutionContext.States.NEW;
                    return strategyContext.component;
                } catch (CircularReferenceException e) {
                    strategyContext.state = ResolutionContext.States.NEW;
                    if (e.metadata().equals(componentMetadata)) {
                        ProvisionStrategy<?> reverseStrategy = e.getReverseStrategy();
                        if (reverseStrategy == null) {
                            throw new IllegalArgumentException("The component [" + componentMetadata.toString()
                                + "] depends on itself");
                        }
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

    private <T> ComponentAdapter<T> componentAdapter(
            final ComponentMetadata<?> metadata,
            final ProvisionStrategy<T> provisionStrategy,
            final Instantiator<T> instantiator,
            final Injector<T> injector) {
        return new ComponentAdapter<T>() {
            @Override public ComponentMetadata metadata() {
                return metadata;
            }
            @Override public ProvisionStrategy<T> provisionStrategy() {
                return provisionStrategy;
            }
            @Override public List<Dependency<?>> dependencies() {
                List<Dependency<?>> dependencies = new LinkedList<>();
                dependencies.addAll(instantiator.dependencies());
                dependencies.addAll(injector.dependencies());
                return dependencies;
            }
        };
    }

}
