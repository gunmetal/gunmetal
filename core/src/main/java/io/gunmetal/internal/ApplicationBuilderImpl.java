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

import io.gunmetal.ApplicationContainer;
import io.gunmetal.ApplicationModule;
import io.gunmetal.Provider;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.Config;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResolutionContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author rees.byars
 */
public class ApplicationBuilderImpl implements ApplicationBuilder {

    @Override public ApplicationContainer build(Class<?> application) {

        ApplicationModule applicationModule = application.getAnnotation(ApplicationModule.class);

        final Config config = new ConfigBuilderImpl().build(applicationModule.options());

        final List<Linker> postWiringLinkers = new LinkedList<>();
        final List<Linker> eagerLinkers = new LinkedList<>();
        Linkers linkers = new Linkers() {
            @Override public void add(Linker linker, LinkingPhase phase) {
                switch (phase) {
                    case POST_WIRING: postWiringLinkers.add(linker); break;
                    case EAGER_INSTANTIATION: eagerLinkers.add(linker); break;
                    default: throw new UnsupportedOperationException("Phase unsupported:  " + phase);
                }

            }
        };

        InjectorFactory injectorFactory = new InjectorFactoryImpl(
                config.qualifierResolver(),
                config.constructorResolver(),
                config.classWalker(),
                linkers);

        final List<ProvisionStrategyDecorator> strategyDecorators = new ArrayList<>();
        strategyDecorators.add(new ScopeDecorator(config.scopeBindings(), linkers));
        ProvisionStrategyDecorator compositeStrategyDecorator = new ProvisionStrategyDecorator() {
            @Override public <T> ProvisionStrategy<T> decorate(
                    ComponentMetadata<?> componentMetadata, ProvisionStrategy<T> delegateStrategy) {
                for (ProvisionStrategyDecorator decorator : strategyDecorators) {
                    delegateStrategy = decorator.decorate(componentMetadata, delegateStrategy);
                }
                return delegateStrategy;
            }
        };

        final ComponentAdapterFactory componentAdapterFactory =
                new ComponentAdapterFactoryImpl(injectorFactory, compositeStrategyDecorator);

        ModuleParser moduleParser = new ModuleParserImpl(
                componentAdapterFactory,
                config.qualifierResolver(),
                config.scopeResolver());

        final Map<Dependency<?>, DependencyRequestHandler<?>> requestHandlers
                = new HashMap<>();

        for (Class<?> module : applicationModule.modules()) {
            List<DependencyRequestHandler<?>> moduleRequestHandlers = moduleParser.parse(module);
            for (DependencyRequestHandler<?> requestHandler : moduleRequestHandlers) {
                for (Dependency<?> dependency : requestHandler.targets()) {
                    DependencyRequestHandler<?> previous = requestHandlers.put(dependency, requestHandler);
                    if (previous != null) {
                        throw new RuntimeException("more than one of type"); // TODO
                    }
                }
            }
        }

        final InternalProvider internalProvider = new InternalProvider() {
            @Override public <T> ProvisionStrategy<? extends T> getProvisionStrategy(final DependencyRequest<T> dependencyRequest) {
                final Dependency<T> dependency = dependencyRequest.dependency();
                DependencyRequestHandler<? extends T> requestHandler = Smithy.cloak(requestHandlers.get(dependency));
                if (requestHandler != null) {
                    return requestHandler
                            .handle(dependencyRequest)
                            .validateResponse()
                            .getProvisionStrategy();
                }
                if (config.isProvider(dependency)) {
                    requestHandler = createProviderHandler(dependencyRequest, config, this, requestHandlers);
                    if (requestHandler != null) {
                        requestHandlers.put(dependency, requestHandler);
                        return requestHandler
                                .handle(dependencyRequest)
                                .validateResponse()
                                .getProvisionStrategy();
                    }
                }
                throw new DependencyException("missing dependency " + dependency.toString()); // TODO
            }
        };

        ResolutionContext linkingContext = ResolutionContext.Factory.create();
        for (Linker linker : postWiringLinkers) {
            linker.link(internalProvider, linkingContext);
        }

        ResolutionContext eagerContext = ResolutionContext.Factory.create();
        for (Linker linker : eagerLinkers) {
            linker.link(internalProvider, eagerContext);
        }

        return new ApplicationContainer() {
            @Override public ApplicationContainer inject(Object injectionTarget) {
                throw new UnsupportedOperationException();
            }

            @Override public <T, D extends io.gunmetal.Dependency<T>> T get(Class<D> dependency) {
                Qualifier qualifier = config.qualifierResolver().resolve(dependency);
                Dependency<T> d = Dependency.from(qualifier, unsafeFirstTypeParam(dependency.getGenericInterfaces()[0]));
                DependencyRequestHandler<? extends T> requestHandler = Smithy.cloak(requestHandlers.get(d));
                if (requestHandler != null) {
                    return requestHandler.force().get(internalProvider, ResolutionContext.Factory.create());
                } else if (config.isProvider(d)) {
                    ProvisionStrategy<T> providerStrategy =
                            createProviderStrategy(d, config, internalProvider, requestHandlers);
                    if (providerStrategy != null) {
                        return providerStrategy.get(internalProvider, ResolutionContext.Factory.create());
                    }
                }
                return null;
            }
        };
    }

    private Type unsafeFirstTypeParam(Type type) {
        return ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    private <T> ProvisionStrategy<T> createProviderStrategy(final ProvisionStrategy<?> componentStrategy,
                                            final Config config,
                                            final InternalProvider internalProvider) {
        final Object provider = config.provider(new Provider<Object>() {
            @Override public Object get() {
                return componentStrategy.get(
                        internalProvider, ResolutionContext.Factory.create());
            }
        });
        return new ProvisionStrategy<T>() {
            @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                return Smithy.cloak(provider);
            }
        };
    }

    private <T> ProvisionStrategy<T> createProviderStrategy(
            final Dependency<T> providerDependency,
            final Config config,
            final InternalProvider internalProvider,
            final Map<Dependency<?>, DependencyRequestHandler<?>> requestHandlers) {
        Type type = unsafeFirstTypeParam(providerDependency.typeKey().type());
        final Dependency<?> componentDependency = Dependency.from(providerDependency.qualifier(), type);
        final DependencyRequestHandler<?> componentHandler = Smithy.cloak(requestHandlers.get(componentDependency));
        if (componentHandler == null) {
            return null;
        }
        ProvisionStrategy<?> componentStrategy = componentHandler.force();
        return createProviderStrategy(componentStrategy, config, internalProvider);
    }

    private <T, C> DependencyRequestHandler<T> createProviderHandler(
            final DependencyRequest<T> providerRequest,
            final Config config,
            final InternalProvider internalProvider,
            final Map<Dependency<?>, DependencyRequestHandler<?>> requestHandlers) {
        Dependency<T> providerDependency = providerRequest.dependency();
        Type type = unsafeFirstTypeParam(providerDependency.typeKey().type());
        final Dependency<C> componentDependency = Dependency.from(providerDependency.qualifier(), type);
        final DependencyRequestHandler<? extends C> componentHandler =
                Smithy.cloak(requestHandlers.get(componentDependency));
        if (componentHandler == null) {
            return null;
        }
        ProvisionStrategy<? extends C> componentStrategy = componentHandler.force();
        final ProvisionStrategy<T> providerStrategy =
                createProviderStrategy(componentStrategy, config, internalProvider);
        return new DependencyRequestHandler<T>() {
            @Override public List<Dependency<?>> targets() {
                return Collections.<Dependency<?>>singletonList(providerRequest.dependency());
            }

            @Override public List<Dependency<?>> dependencies() {
                return componentHandler.dependencies();
            }

            @Override public DependencyResponse<T> handle(DependencyRequest<? super T> dependencyRequest) {
                final DependencyResponse<?> componentResponse =
                        componentHandler.handle(DependencyRequest.Factory.create(providerRequest, componentDependency));
                return new DependencyResponse<T>() {
                    @Override public ValidatedDependencyResponse<T> validateResponse() {
                        componentResponse.validateResponse();
                        return new ValidatedDependencyResponse<T>() {
                            @Override public ProvisionStrategy<T> getProvisionStrategy() {
                                return providerStrategy;
                            }

                            @Override public ValidatedDependencyResponse<T> validateResponse() {
                                return this;
                            }
                        };
                    }
                };
            }

            @Override public ProvisionStrategy<T> force() {
                return providerStrategy;
            }
        };

    }



    //collections - explicit
    //factories - explicit, anonymous + providers
    //deconstructed apis - explicit, anonymous + providers
    //Visitor - Selector  - applicationWalker.walk(new CompositeThingBuilderVisitor());  -  injectable
    //auto add for concrete dependencies


}
