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

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.spi.ResourceMetadata;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
class ResourceFactoryImpl implements ResourceFactory {

    private final InjectorFactory injectorFactory;
    private final boolean requireAcyclic;

    ResourceFactoryImpl(InjectorFactory injectorFactory, boolean requireAcyclic) {
        this.injectorFactory = injectorFactory;
        this.requireAcyclic = requireAcyclic;
    }

    @Override public Resource withClassProvider(ResourceMetadata<Class<?>> resourceMetadata,
                                                ComponentContext context) {
        return resource(
                resourceMetadata,
                context,
                injectorFactory.constructorInstantiator(resourceMetadata, context),
                injectorFactory.compositeInjector(resourceMetadata, context));
    }

    @Override public Resource withProvidedModule(ResourceMetadata<Class<?>> resourceMetadata,
                                                 ComponentContext context) {
        return resource(
                resourceMetadata,
                context,
                injectorFactory.instanceInstantiator(resourceMetadata, context),
                injectorFactory.lazyCompositeInjector(resourceMetadata, context));
    }

    @Override public Resource withMethodProvider(
            ResourceMetadata<Method> resourceMetadata, Dependency moduleDependency, ComponentContext context) {
        return resource(
                resourceMetadata,
                context,
                injectorFactory.methodInstantiator(resourceMetadata, moduleDependency, context),
                injectorFactory.lazyCompositeInjector(resourceMetadata, context));
    }

    @Override public Resource withFieldProvider(
            ResourceMetadata<Field> resourceMetadata, Dependency moduleDependency, ComponentContext context) {
        return resource(
                resourceMetadata,
                context,
                injectorFactory.fieldInstantiator(resourceMetadata, moduleDependency, context),
                injectorFactory.lazyCompositeInjector(resourceMetadata, context));
    }

    private Resource resource(
            final ResourceMetadata<?> metadata,
            ComponentContext context,
            final Instantiator instantiator,
            final Injector injector) {
        ProvisionStrategy provisionStrategy = context.strategyDecorator().decorate(
                metadata,
                baseProvisionStrategy(metadata, instantiator, injector),
                context.linkers());
        return new Resource() {
            @Override public ResourceMetadata<?> metadata() {
                return metadata;
            }

            @Override public ProvisionStrategy provisionStrategy() {
                return provisionStrategy;
            }

            @Override public Resource replicateWith(ComponentContext context) {
                return resource(
                        metadata,
                        context,
                        instantiator.replicateWith(context),
                        injector.replicateWith(context));
            }

            @Override public List<Dependency> dependencies() {
                List<Dependency> dependencies = new LinkedList<>();
                dependencies.addAll(instantiator.dependencies());
                dependencies.addAll(injector.dependencies());
                return dependencies;
            }
        };
    }

    private ProvisionStrategy baseProvisionStrategy(final ResourceMetadata<?> resourceMetadata,
                                                    final Instantiator instantiator,
                                                    final Injector injector) {

        // TODO support needs to be added to allow the override to work
        if (!requireAcyclic || resourceMetadata.overrides().allowCycle()) {
            return cyclicResolutionProvisionStrategy(resourceMetadata, instantiator, injector);
        }

        return (supplier, resolutionContext) -> {
            ResolutionContext.ProvisionContext strategyContext =
                    resolutionContext.provisionContext(resourceMetadata);
            if (strategyContext.state != ResolutionContext.States.NEW) {
                throw new CircularReferenceException(resourceMetadata);
            }
            strategyContext.state = ResolutionContext.States.PRE_INSTANTIATION;
            strategyContext.provision = instantiator.newInstance(supplier, resolutionContext);
            strategyContext.state = ResolutionContext.States.PRE_INJECTION;
            injector.inject(strategyContext.provision, supplier, resolutionContext);
            strategyContext.state = ResolutionContext.States.NEW;
            return strategyContext.provision;
        };

    }

    private ProvisionStrategy cyclicResolutionProvisionStrategy(final ResourceMetadata<?> resourceMetadata,
                                                                final Instantiator instantiator,
                                                                final Injector injector) {
        return new ProvisionStrategy() {
            @Override public Object get(DependencySupplier dependencySupplier, ResolutionContext resolutionContext) {
                ResolutionContext.ProvisionContext strategyContext =
                        resolutionContext.provisionContext(resourceMetadata);
                if (strategyContext.state != ResolutionContext.States.NEW) {
                    if (strategyContext.state == ResolutionContext.States.PRE_INJECTION) {
                        return strategyContext.provision;
                    }
                    throw new CircularReferenceException(resourceMetadata);
                }
                strategyContext.state = ResolutionContext.States.PRE_INSTANTIATION;
                try {
                    strategyContext.provision = instantiator.newInstance(dependencySupplier, resolutionContext);
                    strategyContext.state = ResolutionContext.States.PRE_INJECTION;
                    injector.inject(strategyContext.provision, dependencySupplier, resolutionContext);
                    strategyContext.state = ResolutionContext.States.NEW;
                    return strategyContext.provision;
                } catch (CircularReferenceException e) {
                    strategyContext.state = ResolutionContext.States.NEW;
                    if (e.metadata().equals(resourceMetadata)) {
                        ProvisionStrategy reverseStrategy = e.getReverseStrategy();
                        if (reverseStrategy == null) {
                            throw new RuntimeException(
                                    "The provision [" + resourceMetadata.toString() + "] depends on itself");
                        }
                        if (!strategyContext.attemptedCircularResolution) {
                            strategyContext.attemptedCircularResolution = true;
                            e.getReverseStrategy().get(dependencySupplier, resolutionContext);
                            return strategyContext.provision;
                        }
                    } else if (e.getReverseStrategy() == null) {
                        e.setReverseStrategy(this);
                    }
                    e.push(resourceMetadata);
                    throw e;
                }
            }

        };
    }

}
