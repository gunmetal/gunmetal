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
import io.gunmetal.spi.ResolutionContext;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
class ComponentAdapterFactoryImpl implements ComponentAdapterFactory {

    private final InjectorFactory injectorFactory;
    private final boolean requireAcyclic;

    ComponentAdapterFactoryImpl(InjectorFactory injectorFactory, boolean requireAcyclic) {
        this.injectorFactory = injectorFactory;
        this.requireAcyclic = requireAcyclic;
    }

    @Override public <T> ComponentAdapter<T> withClassProvider(ComponentMetadata<Class<?>> componentMetadata,
                                                               GraphContext context) {
        return componentAdapter(
                componentMetadata,
                context,
                injectorFactory.constructorInstantiator(componentMetadata, context),
                injectorFactory.compositeInjector(componentMetadata, context));
    }

    @Override public <T> ComponentAdapter<T> withMethodProvider(ComponentMetadata<Method> componentMetadata,
                                                                GraphContext context) {
        return componentAdapter(
                componentMetadata,
                context,
                injectorFactory.methodInstantiator(componentMetadata, context),
                injectorFactory.lazyCompositeInjector(componentMetadata, context));
    }

    @Override public <T> ComponentAdapter<T> withStatefulMethodProvider(ComponentMetadata<Method> componentMetadata,
                                                                        GraphContext context) {
        return componentAdapter(
                componentMetadata,
                context,
                injectorFactory.statefulMethodInstantiator(componentMetadata, context),
                injectorFactory.lazyCompositeInjector(componentMetadata, context));
    }

    private <T> ComponentAdapter<T> componentAdapter(
            final ComponentMetadata<?> metadata,
            GraphContext context,
            final Instantiator<T> instantiator,
            final Injector<T> injector) {
        ProvisionStrategy<T> provisionStrategy = context.strategyDecorator().decorate(
                metadata,
                baseProvisionStrategy(metadata, instantiator, injector, context),
                context.linkers());
        return new ComponentAdapter<T>() {
            @Override public ComponentMetadata<?> metadata() {
                return metadata;
            }
            @Override public ProvisionStrategy<T> provisionStrategy() {
                return provisionStrategy;
            }
            @Override public ComponentAdapter<T> replicateWith(GraphContext context) {
                return componentAdapter(
                        metadata,
                        context,
                        instantiator.replicateWith(context),
                        injector.replicateWith(context));
            }
            @Override public List<Dependency<?>> dependencies() {
                List<Dependency<?>> dependencies = new LinkedList<>();
                dependencies.addAll(instantiator.dependencies());
                dependencies.addAll(injector.dependencies());
                return dependencies;
            }
        };
    }

    private <T> ProvisionStrategy<T> baseProvisionStrategy(final ComponentMetadata<?> componentMetadata,
                                                           final Instantiator<T> instantiator,
                                                           final Injector<T> injector,
                                                           GraphContext context) {

        if (!requireAcyclic || componentMetadata.overrides().allowCycle()) {
            return cyclicResolutionProvisionStrategy(componentMetadata, instantiator, injector, context);
        }

        return (internalProvider, resolutionContext) -> {
            ResolutionContext.ProvisionContext<T> strategyContext =
                    resolutionContext.provisionContext(componentMetadata);
            if (strategyContext.state != ResolutionContext.States.NEW) {
                throw new CircularReferenceException(componentMetadata);
            }
            strategyContext.state = ResolutionContext.States.PRE_INSTANTIATION;
            strategyContext.component = instantiator.newInstance(internalProvider, resolutionContext);
            strategyContext.state = ResolutionContext.States.PRE_INJECTION;
            injector.inject(strategyContext.component, internalProvider, resolutionContext);
            strategyContext.state = ResolutionContext.States.NEW;
            return strategyContext.component;
        };

    }

    private <T> ProvisionStrategy<T> cyclicResolutionProvisionStrategy(final ComponentMetadata<?> componentMetadata,
                                                           final Instantiator<T> instantiator,
                                                           final Injector<T> injector,
                                                           final GraphContext context) {
        return new ProvisionStrategy<T>() {
            @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                ResolutionContext.ProvisionContext<T> strategyContext =
                        resolutionContext.provisionContext(componentMetadata);
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
                            context.errors().add(
                                    "The component [" + componentMetadata.toString() + "] depends on itself");
                        }
                        e.getReverseStrategy().get(internalProvider, resolutionContext);
                        return strategyContext.component;
                    } else if (e.getReverseStrategy() == null) {
                        e.setReverseStrategy(this);
                    }
                    e.push(componentMetadata);
                    throw e;
                }
            }

        };
    }

}
