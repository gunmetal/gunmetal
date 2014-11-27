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

import io.gunmetal.Inject;
import io.gunmetal.TemplateGraph;
import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.spi.InjectionResolver;
import io.gunmetal.spi.ProviderAdapter;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Scope;
import io.gunmetal.spi.Scopes;
import io.gunmetal.spi.impl.AnnotationInjectionResolver;
import io.gunmetal.spi.impl.ExactlyOneConstructorResolver;
import io.gunmetal.spi.impl.GunmetalProviderAdapter;
import io.gunmetal.spi.impl.Jsr330ProviderAdapter;
import io.gunmetal.spi.impl.LeastGreedyConstructorResolver;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
public final class GraphBuilder {

    private GraphConfig graphConfig;
    private Graph parentGraph;

    public GraphBuilder() {
        Map<Scope, ProvisionStrategyDecorator> scopeDecorators = new HashMap<>();
        scopeDecorators.put(Scopes.UNDEFINED, ProvisionStrategyDecorator::none);
        graphConfig = new GraphConfig(
                new MutableGraphMetadata(),
                new AnnotationInjectionResolver(Inject.class),
                new ConfigurableMetadataResolver(),
                new LeastGreedyConstructorResolver(),
                new GunmetalProviderAdapter(),
                scopeDecorators);
    }

    GraphBuilder(Graph parentGraph, GraphConfig graphConfig) {
        this.parentGraph = parentGraph;
        this.graphConfig = new GraphConfig(graphConfig);
    }

    public GraphBuilder requireQualifiers() {
        graphConfig.getConfigurableMetadataResolver().requireQualifiers(true);
        return this;
    }

    public GraphBuilder restrictPluralQualifiers() {
        graphConfig.getConfigurableMetadataResolver().restrictPluralQualifiers(true);
        return this;
    }

    public GraphBuilder requireInterfaces() {
        graphConfig.getGraphMetadata().setRequireInterfaces(true);
        return this;
    }

    public GraphBuilder requireAcyclic() {
        graphConfig.getGraphMetadata().setRequireAcyclic(true);
        return this;
    }

    public GraphBuilder requireExplicitModuleDependencies() {
        graphConfig.getGraphMetadata().setRequireExplicitModuleDependencies(true);
        return this;
    }

    public GraphBuilder restrictFieldInjection() {
        graphConfig.getGraphMetadata().setRestrictFieldInjection(true);
        return this;
    }

    public GraphBuilder restrictSetterInjection() {
        graphConfig.getGraphMetadata().setRestrictSetterInjection(true);
        return this;
    }

    public GraphBuilder withQualifierType(Class<? extends Annotation> qualifierType) {
        graphConfig.getConfigurableMetadataResolver().qualifierType(qualifierType);
        return this;
    }

    public GraphBuilder withEagerType(Class<? extends Annotation> eagerType, boolean indicatesEager) {
        graphConfig.getConfigurableMetadataResolver().eagerType(eagerType, indicatesEager);
        return this;
    }

    public GraphBuilder addScope(Class<? extends Annotation> scopeType,
                                 Scope scope,
                                 ProvisionStrategyDecorator scopeDecorator) {
        graphConfig.getConfigurableMetadataResolver().addScope(scopeType, scope);
        graphConfig.getScopeDecorators().put(scope, scopeDecorator);
        return this;
    }

    public GraphBuilder withJsr330Metadata() {
        graphConfig.getConfigurableMetadataResolver()
                .scopeType(javax.inject.Scope.class)
                .addScope(javax.inject.Singleton.class, Scopes.SINGLETON)
                .addScope(null, Scopes.PROTOTYPE)
                .qualifierType(javax.inject.Qualifier.class)
                .restrictPluralQualifiers(true);
        return withInjectionResolver(new AnnotationInjectionResolver(javax.inject.Inject.class))
                .withConstructorResolver(new ExactlyOneConstructorResolver(graphConfig.getInjectionResolver()))
                .withProviderAdapter(new Jsr330ProviderAdapter());
    }

    public GraphBuilder withInjectionResolver(InjectionResolver injectionResolver) {
        graphConfig.setInjectionResolver(injectionResolver);
        return this;
    }

    public GraphBuilder withConstructorResolver(ConstructorResolver constructorResolver) {
        graphConfig.setConstructorResolver(constructorResolver);
        return this;
    }

    public GraphBuilder withProviderAdapter(ProviderAdapter providerAdapter) {
        graphConfig.setProviderAdapter(providerAdapter);
        return this;
    }

    public TemplateGraph buildTemplate(Class<?>... modules) {
        return GraphTemplate.buildTemplate(parentGraph, graphConfig, modules);
    }

}
