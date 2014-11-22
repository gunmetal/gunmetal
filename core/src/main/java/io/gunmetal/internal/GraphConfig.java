package io.gunmetal.internal;

import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.spi.InjectionResolver;
import io.gunmetal.spi.ProviderAdapter;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Scope;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
class GraphConfig {

    private MutableGraphMetadata graphMetadata;
    private InjectionResolver injectionResolver;
    private ConfigurableMetadataResolver configurableMetadataResolver;
    private ConstructorResolver constructorResolver;
    private ProviderAdapter providerAdapter;
    private Map<Scope, ProvisionStrategyDecorator> scopeDecorators;

    GraphConfig(
            MutableGraphMetadata graphMetadata,
            InjectionResolver injectionResolver,
            ConfigurableMetadataResolver configurableMetadataResolver,
            ConstructorResolver constructorResolver,
            ProviderAdapter providerAdapter,
            Map<Scope, ProvisionStrategyDecorator> scopeDecorators) {
        this.graphMetadata = graphMetadata;
        this.injectionResolver = injectionResolver;
        this.configurableMetadataResolver = configurableMetadataResolver;
        this.constructorResolver = constructorResolver;
        this.providerAdapter = providerAdapter;
        this.scopeDecorators = scopeDecorators;
    }

    GraphConfig(GraphConfig graphConfig) {
        this.graphMetadata = graphConfig.graphMetadata.replicate();
        this.injectionResolver = graphConfig.injectionResolver;
        this.configurableMetadataResolver = graphConfig.configurableMetadataResolver.replicate();
        this.constructorResolver = graphConfig.constructorResolver;
        this.providerAdapter = graphConfig.providerAdapter;
        this.scopeDecorators = new HashMap<>(graphConfig.scopeDecorators);
    }

    public MutableGraphMetadata getGraphMetadata() {
        return graphMetadata;
    }

    public void setGraphMetadata(MutableGraphMetadata graphMetadata) {
        this.graphMetadata = graphMetadata;
    }

    public InjectionResolver getInjectionResolver() {
        return injectionResolver;
    }

    public void setInjectionResolver(InjectionResolver injectionResolver) {
        this.injectionResolver = injectionResolver;
    }

    public ConfigurableMetadataResolver getConfigurableMetadataResolver() {
        return configurableMetadataResolver;
    }

    public void setConfigurableMetadataResolver(ConfigurableMetadataResolver configurableMetadataResolver) {
        this.configurableMetadataResolver = configurableMetadataResolver;
    }

    public ConstructorResolver getConstructorResolver() {
        return constructorResolver;
    }

    public void setConstructorResolver(ConstructorResolver constructorResolver) {
        this.constructorResolver = constructorResolver;
    }

    public ProviderAdapter getProviderAdapter() {
        return providerAdapter;
    }

    public void setProviderAdapter(ProviderAdapter providerAdapter) {
        this.providerAdapter = providerAdapter;
    }

    public Map<Scope, ProvisionStrategyDecorator> getScopeDecorators() {
        return scopeDecorators;
    }

    public void setScopeDecorators(Map<Scope, ProvisionStrategyDecorator> scopeDecorators) {
        this.scopeDecorators = scopeDecorators;
    }

}
