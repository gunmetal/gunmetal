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

import io.gunmetal.spi.ProvisionMetadata;
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
class ProvisionAdapterFactoryImpl implements ProvisionAdapterFactory {

    private final InjectorFactory injectorFactory;
    private final boolean requireAcyclic;

    ProvisionAdapterFactoryImpl(InjectorFactory injectorFactory, boolean requireAcyclic) {
        this.injectorFactory = injectorFactory;
        this.requireAcyclic = requireAcyclic;
    }

    @Override public <T> ProvisionAdapter<T> withClassProvider(ProvisionMetadata<Class<?>> provisionMetadata,
                                                               GraphContext context) {
        return provisionAdapter(
                provisionMetadata,
                context,
                injectorFactory.constructorInstantiator(provisionMetadata, context),
                injectorFactory.compositeInjector(provisionMetadata, context));
    }

    @Override public <T> ProvisionAdapter<T> withMethodProvider(ProvisionMetadata<Method> provisionMetadata,
                                                                GraphContext context) {
        return provisionAdapter(
                provisionMetadata,
                context,
                injectorFactory.methodInstantiator(provisionMetadata, context),
                injectorFactory.lazyCompositeInjector(provisionMetadata, context));
    }

    @Override public <T> ProvisionAdapter<T> withStatefulMethodProvider(ProvisionMetadata<Method> provisionMetadata,
                                                                        Dependency<?> moduleDependency,
                                                                        GraphContext context) {
        return provisionAdapter(
                provisionMetadata,
                context,
                injectorFactory.statefulMethodInstantiator(provisionMetadata, moduleDependency, context),
                injectorFactory.lazyCompositeInjector(provisionMetadata, context));
    }

    @Override public <T> ProvisionAdapter<T> withProvidedModule(ProvisionMetadata<Class<?>> provisionMetadata,
                                                                GraphContext context) {
        return provisionAdapter(
                provisionMetadata,
                context,
                injectorFactory.instanceInstantiator(provisionMetadata, context),
                injectorFactory.lazyCompositeInjector(provisionMetadata, context));
    }

    private <T> ProvisionAdapter<T> provisionAdapter(
            final ProvisionMetadata<?> metadata,
            GraphContext context,
            final Instantiator<T> instantiator,
            final Injector<T> injector) {
        ProvisionStrategy<T> provisionStrategy = context.strategyDecorator().decorate(
                metadata,
                baseProvisionStrategy(metadata, instantiator, injector, context),
                context.linkers());
        return new ProvisionAdapter<T>() {
            @Override public ProvisionMetadata<?> metadata() {
                return metadata;
            }
            @Override public ProvisionStrategy<T> provisionStrategy() {
                return provisionStrategy;
            }
            @Override public ProvisionAdapter<T> replicateWith(GraphContext context) {
                return provisionAdapter(
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

    private <T> ProvisionStrategy<T> baseProvisionStrategy(final ProvisionMetadata<?> provisionMetadata,
                                                           final Instantiator<T> instantiator,
                                                           final Injector<T> injector,
                                                           GraphContext context) {

        // TODO support needs to be added to allow the override to work
        if (!requireAcyclic || provisionMetadata.overrides().allowCycle()) {
            return cyclicResolutionProvisionStrategy(provisionMetadata, instantiator, injector, context);
        }

        return (internalProvider, resolutionContext) -> {
            ResolutionContext.ProvisionContext<T> strategyContext =
                    resolutionContext.provisionContext(provisionMetadata);
            if (strategyContext.state != ResolutionContext.States.NEW) {
                throw new CircularReferenceException(provisionMetadata);
            }
            strategyContext.state = ResolutionContext.States.PRE_INSTANTIATION;
            strategyContext.provision = instantiator.newInstance(internalProvider, resolutionContext);
            strategyContext.state = ResolutionContext.States.PRE_INJECTION;
            injector.inject(strategyContext.provision, internalProvider, resolutionContext);
            strategyContext.state = ResolutionContext.States.NEW;
            return strategyContext.provision;
        };

    }

    private <T> ProvisionStrategy<T> cyclicResolutionProvisionStrategy(final ProvisionMetadata<?> provisionMetadata,
                                                           final Instantiator<T> instantiator,
                                                           final Injector<T> injector,
                                                           final GraphContext context) {
        return new ProvisionStrategy<T>() {
            @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                ResolutionContext.ProvisionContext<T> strategyContext =
                        resolutionContext.provisionContext(provisionMetadata);
                if (strategyContext.state != ResolutionContext.States.NEW) {
                    if (strategyContext.state == ResolutionContext.States.PRE_INJECTION) {
                        return strategyContext.provision;
                    }
                    throw new CircularReferenceException(provisionMetadata);
                }
                strategyContext.state = ResolutionContext.States.PRE_INSTANTIATION;
                try {
                    strategyContext.provision = instantiator.newInstance(internalProvider, resolutionContext);
                    strategyContext.state = ResolutionContext.States.PRE_INJECTION;
                    injector.inject(strategyContext.provision, internalProvider, resolutionContext);
                    strategyContext.state = ResolutionContext.States.NEW;
                    return strategyContext.provision;
                } catch (CircularReferenceException e) {
                    strategyContext.state = ResolutionContext.States.NEW;
                    if (e.metadata().equals(provisionMetadata)) {
                        ProvisionStrategy<?> reverseStrategy = e.getReverseStrategy();
                        if (reverseStrategy == null) {
                            context.errors().add(
                                    "The provision [" + provisionMetadata.toString() + "] depends on itself");
                        }
                        e.getReverseStrategy().get(internalProvider, resolutionContext);
                        return strategyContext.provision;
                    } else if (e.getReverseStrategy() == null) {
                        e.setReverseStrategy(this);
                    }
                    e.push(provisionMetadata);
                    throw e;
                }
            }

        };
    }

}
