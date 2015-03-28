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
import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.spi.ConverterSupplier;
import io.gunmetal.spi.InjectionResolver;
import io.gunmetal.spi.SupplierAdapter;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Scope;
import io.gunmetal.spi.Scopes;
import io.gunmetal.spi.impl.AnnotationInjectionResolver;
import io.gunmetal.spi.impl.ExactlyOneConstructorResolver;
import io.gunmetal.spi.impl.DefaultSupplierAdapter;
import io.gunmetal.spi.impl.Jsr330SupplierAdapter;
import io.gunmetal.spi.impl.LeastGreedyConstructorResolver;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
public final class ComponentBuilder {

    private ComponentConfig componentConfig;
    private ComponentGraph parentComponentGraph;

    public ComponentBuilder() {
        Map<Scope, ProvisionStrategyDecorator> scopeDecorators = new HashMap<>();
        scopeDecorators.put(Scopes.UNDEFINED, ProvisionStrategyDecorator::none);
        componentConfig = new ComponentConfig(
                new ComponentSettings(),
                new AnnotationInjectionResolver(Inject.class),
                new ConfigurableMetadataResolver(),
                new LeastGreedyConstructorResolver(),
                new DefaultSupplierAdapter(),
                to -> Collections.emptyList(),
                scopeDecorators);
    }

    ComponentBuilder(ComponentGraph parentComponentGraph,
                     ComponentConfig componentConfig) {
        this.parentComponentGraph = parentComponentGraph;
        this.componentConfig = new ComponentConfig(componentConfig);
    }

    public ComponentBuilder requireQualifiers() {
        componentConfig.getConfigurableMetadataResolver().requireQualifiers(true);
        return this;
    }

    public ComponentBuilder restrictPluralQualifiers() {
        componentConfig.getConfigurableMetadataResolver().restrictPluralQualifiers(true);
        return this;
    }

    public ComponentBuilder requireInterfaces() {
        componentConfig.getComponentSettings().setRequireInterfaces(true);
        return this;
    }

    public ComponentBuilder requireAcyclic() {
        componentConfig.getComponentSettings().setRequireAcyclic(true);
        return this;
    }

    public ComponentBuilder requireExplicitModuleDependencies() {
        componentConfig.getComponentSettings().setRequireExplicitModuleDependencies(true);
        return this;
    }

    public ComponentBuilder restrictFieldInjection() {
        componentConfig.getComponentSettings().setRestrictFieldInjection(true);
        return this;
    }

    public ComponentBuilder restrictSetterInjection() {
        componentConfig.getComponentSettings().setRestrictSetterInjection(true);
        return this;
    }

    public ComponentBuilder withQualifierType(Class<? extends Annotation> qualifierType) {
        componentConfig.getConfigurableMetadataResolver().qualifierType(qualifierType);
        return this;
    }

    public ComponentBuilder withEagerType(Class<? extends Annotation> eagerType, boolean indicatesEager) {
        componentConfig.getConfigurableMetadataResolver().eagerType(eagerType, indicatesEager);
        return this;
    }

    public ComponentBuilder addScope(Class<? extends Annotation> scopeType,
                                 Scope scope,
                                 ProvisionStrategyDecorator scopeDecorator) {
        componentConfig.getConfigurableMetadataResolver().addScope(scopeType, scope);
        componentConfig.getScopeDecorators().put(scope, scopeDecorator);
        return this;
    }

    public ComponentBuilder withJsr330Metadata() {
        componentConfig.getConfigurableMetadataResolver()
                .scopeType(javax.inject.Scope.class)
                .addScope(javax.inject.Singleton.class, Scopes.SINGLETON)
                .addScope(null, Scopes.PROTOTYPE)
                .qualifierType(javax.inject.Qualifier.class)
                .restrictPluralQualifiers(true);
        return withInjectionResolver(new AnnotationInjectionResolver(javax.inject.Inject.class))
                .withConstructorResolver(new ExactlyOneConstructorResolver(componentConfig.getInjectionResolver()))
                .withSupplierAdapter(new Jsr330SupplierAdapter());
    }

    public ComponentBuilder withInjectionResolver(InjectionResolver injectionResolver) {
        componentConfig.setInjectionResolver(injectionResolver);
        return this;
    }

    public ComponentBuilder withConstructorResolver(ConstructorResolver constructorResolver) {
        componentConfig.setConstructorResolver(constructorResolver);
        return this;
    }

    public ComponentBuilder withSupplierAdapter(SupplierAdapter supplierAdapter) {
        componentConfig.setSupplierAdapter(supplierAdapter);
        return this;
    }

    public ComponentBuilder withConverterSupplier(ConverterSupplier converterSupplier) {
        componentConfig.setConverterSupplier(converterSupplier);
        return this;
    }

    public <T> T build(Class<T> componentFactoryInterface) {
        return ComponentTemplate.buildTemplate(parentComponentGraph, componentConfig, componentFactoryInterface);
    }

}
