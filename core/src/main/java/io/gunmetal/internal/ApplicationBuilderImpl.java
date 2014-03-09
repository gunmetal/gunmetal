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
import io.gunmetal.Gunmetal;
import io.gunmetal.Provider;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.Config;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.ResolutionContext;
import io.gunmetal.spi.Scope;
import io.gunmetal.spi.Scopes;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
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

        final Map<Dependency<?>, ComponentAdapterProvider<?>> componentAdapterProviders
                = new HashMap<>();

        for (Class<?> module : applicationModule.modules()) {

            List<ComponentAdapterProvider<?>> moduleAdapterProviders = moduleParser.parse(module);

            for (ComponentAdapterProvider<?> adapterProvider : moduleAdapterProviders) {
                ComponentAdapter<?> adapter = adapterProvider.get();
                final ComponentMetadata<?> metadata = adapter.metadata();
                for (final Dependency<?> dependency : metadata.targets()) {
                    ComponentAdapterProvider<?> previous = componentAdapterProviders.put(dependency, adapterProvider);
                    if (previous != null) {
                        throw new RuntimeException("more than one of type"); // TODO
                    }

                }
            }
        }

        final InternalProvider internalProvider = new InternalProvider() {
            @Override public <T> ProvisionStrategy<T> getProvisionStrategy(final DependencyRequest dependencyRequest) {
                final Dependency<?> dependency = dependencyRequest.dependency();
                ComponentAdapterProvider<T> adapterProvider =
                        Smithy.cloak(componentAdapterProviders.get(dependency));
                if (adapterProvider != null) {
                    if (!adapterProvider.isAccessibleTo(dependencyRequest)
                            && dependencyRequest.sourceOrigin() != Gunmetal.class) {
                        StringBuilder message = new StringBuilder();
                        for (String error : dependencyRequest.errors()) {
                            message.append(error).append("\n");
                        }
                        throw new IllegalAccessError(message.toString());
                    }
                    return adapterProvider.get().provisionStrategy();
                }
                if (config.isProvider(dependency)) {
                    // TODO add check before casting
                    ParameterizedType parameterizedType = (ParameterizedType) dependency.typeKey().type();
                    Type type = parameterizedType.getActualTypeArguments()[0];
                    // TODO store
                    Dependency<?> providedTypeDependency = Dependency.from(dependency.qualifier(), type);
                    ComponentAdapterProvider<?> providedTypeAdapterProvider =
                            Smithy.cloak(componentAdapterProviders.get(providedTypeDependency));
                    if (providedTypeAdapterProvider != null) {
                        if (!providedTypeAdapterProvider.isAccessibleTo(dependencyRequest)
                                && dependencyRequest.sourceOrigin() != Gunmetal.class) {
                            StringBuilder message = new StringBuilder();
                            for (String error : dependencyRequest.errors()) { //TODO this sucks
                                message.append(error).append("\n");
                            }
                            throw new IllegalAccessError(message.toString());
                        }
                        final ProvisionStrategy<?> providedTypeProvisionStrategy =
                                providedTypeAdapterProvider.get().provisionStrategy();
                        final InternalProvider internalProvider = this;
                        final Object provider = config.provider(new Provider() {
                            @Override public Object get() {
                                return providedTypeProvisionStrategy.get(internalProvider, ResolutionContext.Factory.create());
                            }
                        });
                        return new ProvisionStrategy<T>() {
                            @Override public T get(InternalProvider internalProvider, ResolutionContext resolutionContext) {
                                return Smithy.cloak(provider);
                            }
                        };
                    }
                }
                throw new RuntimeException("missing dependency " + dependency.toString()); // TODO
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
                final Qualifier qualifier = config.qualifierResolver().resolve(dependency);
                ParameterizedType parameterizedType = (ParameterizedType) dependency.getGenericInterfaces()[0];
                Type type = parameterizedType.getActualTypeArguments()[0];
                final Dependency<?> d = Dependency.from(qualifier, type);
                return Smithy.cloak(internalProvider.getProvisionStrategy(DependencyRequest.Factory.create(
                        new ComponentMetadata<Class>() {
                            @Override public Class provider() {
                                return Gunmetal.class;
                            }
                            @Override public Class<?> providerClass() {
                                return Gunmetal.class;
                            }
                            @Override public ModuleMetadata moduleMetadata() {
                                return new ModuleMetadata() {
                                    @Override public Class<?> moduleClass() {
                                        return Gunmetal.class;
                                    }
                                    @Override public Qualifier qualifier() {
                                        return Qualifier.NONE;
                                    }

                                    @Override public Class<?>[] referencedModules() {
                                        return new Class<?>[0];
                                    }
                                };
                            }
                            @Override public Qualifier qualifier() {
                                return Qualifier.NONE;
                            }
                            @Override public Collection<Dependency<?>> targets() {
                                return Collections.emptySet();
                            }
                            @Override public Scope scope() {
                                return Scopes.EAGER_SINGLETON;
                            }
                        }, d)).get(internalProvider, ResolutionContext.Factory.create()));
            }
        };
    }

    //collections - explicit
    //factories - explicit, anonymous + providers
    //deconstructed apis - explicit, anonymous + providers
    //Visitor - Selector  - applicationWalker.walk(new CompositeThingBuilderVisitor());  -  injectable
    //auto add for concrete dependencies


}
