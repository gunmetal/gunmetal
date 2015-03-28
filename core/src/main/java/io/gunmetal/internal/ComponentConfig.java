package io.gunmetal.internal;

import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.spi.ConverterSupplier;
import io.gunmetal.spi.InjectionResolver;
import io.gunmetal.spi.SupplierAdapter;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.Scope;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rees.byars
 */
class ComponentConfig {

    private ComponentSettings componentSettings;
    private InjectionResolver injectionResolver;
    private ConfigurableMetadataResolver configurableMetadataResolver;
    private ConstructorResolver constructorResolver;
    private SupplierAdapter supplierAdapter;
    private ConverterSupplier converterSupplier;
    private Map<Scope, ProvisionStrategyDecorator> scopeDecorators;

    ComponentConfig(
            ComponentSettings componentSettings,
            InjectionResolver injectionResolver,
            ConfigurableMetadataResolver configurableMetadataResolver,
            ConstructorResolver constructorResolver,
            SupplierAdapter supplierAdapter,
            ConverterSupplier converterSupplier,
            Map<Scope, ProvisionStrategyDecorator> scopeDecorators) {
        this.componentSettings = componentSettings;
        this.injectionResolver = injectionResolver;
        this.configurableMetadataResolver = configurableMetadataResolver;
        this.constructorResolver = constructorResolver;
        this.supplierAdapter = supplierAdapter;
        this.converterSupplier = converterSupplier;
        this.scopeDecorators = scopeDecorators;
    }

    ComponentConfig(ComponentConfig componentConfig) {
        this.componentSettings = componentConfig.componentSettings.replicate();
        this.injectionResolver = componentConfig.injectionResolver;
        this.configurableMetadataResolver = componentConfig.configurableMetadataResolver.replicate();
        this.constructorResolver = componentConfig.constructorResolver;
        this.supplierAdapter = componentConfig.supplierAdapter;
        this.converterSupplier = componentConfig.converterSupplier;
        this.scopeDecorators = new HashMap<>(componentConfig.scopeDecorators);
    }

    public ComponentSettings getComponentSettings() {
        return componentSettings;
    }

    public void setComponentSettings(ComponentSettings componentSettings) {
        this.componentSettings = componentSettings;
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

    public SupplierAdapter getSupplierAdapter() {
        return supplierAdapter;
    }

    public void setSupplierAdapter(SupplierAdapter supplierAdapter) {
        this.supplierAdapter = supplierAdapter;
    }

    public ConverterSupplier getConverterSupplier() {
        return converterSupplier;
    }

    public void setConverterSupplier(ConverterSupplier converterSupplier) {
        this.converterSupplier = converterSupplier;
    }

    public Map<Scope, ProvisionStrategyDecorator> getScopeDecorators() {
        return scopeDecorators;
    }

    public void setScopeDecorators(Map<Scope, ProvisionStrategyDecorator> scopeDecorators) {
        this.scopeDecorators = scopeDecorators;
    }

}
