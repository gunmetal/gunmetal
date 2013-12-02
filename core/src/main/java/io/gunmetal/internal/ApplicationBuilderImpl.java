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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
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

        final Config config = new ConfigBuildImpl().build(applicationModule.options());

        final List<Linker> postWiringLinkers = new LinkedList<Linker>();
        final List<Linker> eagerLinkers = new LinkedList<Linker>();
        Linkers linkers = new Linkers() {
            @Override public void add(Linker linker, LinkingPhase phase) {
                switch (phase) {
                    case POST_WIRING: postWiringLinkers.add(linker);
                    case EAGER_INSTANTIATION: eagerLinkers.add(linker);
                }

            }
        };

        InjectorFactory injectorFactory = new InjectorFactoryImpl(
                config.qualifierResolver(),
                config.constructorResolver(),
                config.classWalker(),
                linkers);

        final List<ProvisionStrategyDecorator> strategyDecorators = new ArrayList<ProvisionStrategyDecorator>();
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

        ComponentAdapterFactory componentAdapterFactory =
                new ComponentAdapterFactoryImpl(injectorFactory, compositeStrategyDecorator);

        ModuleParser moduleParser = new ModuleParserImpl(
                componentAdapterFactory,
                config.qualifierResolver(),
                config.scopeResolver());

        final Map<Dependency<?>, ComponentAdapterProvider<?>> componentAdapterProviders
                = new HashMap<Dependency<?>, ComponentAdapterProvider<?>>();

        for (Class<?> module : applicationModule.modules()) {

            List<ComponentAdapterProvider<?>> moduleAdapterProviders = moduleParser.parse(module);

            for (ComponentAdapterProvider<?> adapterProvider : moduleAdapterProviders) {
                ComponentAdapter<?> adapter = adapterProvider.get();
                final ComponentMetadata<?> metadata = adapter.metadata();
                for (final Dependency<?> dependency : metadata.targets()) {
                    componentAdapterProviders.put(dependency, adapterProvider);
                }
            }
        }

        final InternalProvider internalProvider = new InternalProvider() {
            @Override public <T> ProvisionStrategy<T> getProvisionStrategy(DependencyRequest dependencyRequest) {
                ComponentAdapterProvider<T> adapterProvider =
                        Smithy.cloak(componentAdapterProviders.get(dependencyRequest.dependency()));

                if (!adapterProvider.isAccessibleTo(dependencyRequest)) {
                    throw new IllegalAccessError(); // TODO
                }
                return adapterProvider.get().provisionStrategy();
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
                ComponentAdapterProvider<T> adapterProvider = Smithy.cloak(
                        componentAdapterProviders.get(Dependency.from(qualifier, type)));
                return adapterProvider.get()
                        .provisionStrategy().get(internalProvider, ResolutionContext.Factory.create());
            }
        };
    }

}
