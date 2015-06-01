package io.gunmetal.spi;

import io.gunmetal.AccessLevel;
import io.gunmetal.Inject;
import io.gunmetal.Lazy;
import io.gunmetal.Module;
import io.gunmetal.Singleton;
import io.gunmetal.spi.impl.AnnotationInjectionResolver;
import io.gunmetal.spi.impl.ConfigurableMetadataResolver;
import io.gunmetal.spi.impl.DefaultSupplierAdapter;
import io.gunmetal.spi.impl.ExactlyOneConstructorResolver;
import io.gunmetal.spi.impl.Jsr330SupplierAdapter;
import io.gunmetal.spi.impl.LeastGreedyConstructorResolver;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rees.byars
 */
@Module(access = AccessLevel.PRIVATE)
public interface GunmetalComponent {

    InjectionResolver injectionResolver();

    QualifierResolver qualifierResolver();

    ResourceMetadataResolver resourceMetadataResolver();

    ConstructorResolver constructorResolver();

    SupplierAdapter supplierAdapter();

    ConverterSupplier converterSupplier();

    Map<Scope, ProvisionStrategyDecorator> scopeDecorators();

    List<ProvisionStrategyDecorator> strategyDecorators();

    Set<Option> options();

    List<RequestVisitor> requestVisitors();

    class Default implements GunmetalComponent {

        private final InjectionResolver injectionResolver = new AnnotationInjectionResolver(Inject.class);
        private final Set<Option> options;
        private final ConfigurableMetadataResolver metadataResolver;
        private final ConstructorResolver constructorResolver = new LeastGreedyConstructorResolver();
        private final SupplierAdapter supplierAdapter = new DefaultSupplierAdapter();
        private final ConverterSupplier converterSupplier = to -> Collections.emptyList();
        private final Map<Scope, ProvisionStrategyDecorator> scopeDecorators;
        private final List<ProvisionStrategyDecorator> strategyDecorators = new ArrayList<>();
        private final List<RequestVisitor> requestVisitors = new ArrayList<>();
        private final Map<Class<? extends Annotation>, Scope> scopeMap = new HashMap<>();

        public Default(Option ... options) {

            this.options = new HashSet<>();
            Collections.addAll(this.options, options);

            scopeDecorators = new HashMap<>();
            scopeDecorators.put(Scopes.UNDEFINED, ProvisionStrategyDecorator::none);

            scopeMap.put(Singleton.class, Scopes.SINGLETON);
            scopeMap.put(null, Scopes.PROTOTYPE);

            metadataResolver =
                    new ConfigurableMetadataResolver(
                            io.gunmetal.Qualifier.class,
                            Lazy.class,
                            false,
                            io.gunmetal.Scope.class,
                            scopeMap,
                            this.options.contains(Option.REQUIRE_QUALIFIERS),
                            this.options.contains(Option.RESTRICT_PLURAL_QUALIFIERS));

        }

        @Override public InjectionResolver injectionResolver() {
            return injectionResolver;
        }

        @Override public QualifierResolver qualifierResolver() {
            return metadataResolver;
        }

        @Override public ResourceMetadataResolver resourceMetadataResolver() {
            return metadataResolver;
        }

        @Override public ConstructorResolver constructorResolver() {
            return constructorResolver;
        }

        @Override public SupplierAdapter supplierAdapter() {
            return supplierAdapter;
        }

        @Override public ConverterSupplier converterSupplier() {
            return converterSupplier;
        }

        @Override public Map<Scope, ProvisionStrategyDecorator> scopeDecorators() {
            return scopeDecorators;
        }

        @Override public List<ProvisionStrategyDecorator> strategyDecorators() {
            return strategyDecorators;
        }

        @Override public Set<Option> options() {
            return options;
        }

        @Override public List<RequestVisitor> requestVisitors() {
            return requestVisitors;
        }

        public Default addScope(
                Class<? extends Annotation> scopeAnnotationType,
                Scope scope,
                ProvisionStrategyDecorator scopeDecorator) {
            scopeMap.put(scopeAnnotationType, scope);
            scopeDecorators.put(scope, scopeDecorator);
            return this;
        }

    }

    class Jsr330 implements GunmetalComponent {

        private final InjectionResolver injectionResolver = new AnnotationInjectionResolver(javax.inject.Inject.class);
        private final Set<Option> options;
        private final ConfigurableMetadataResolver metadataResolver;
        private final ConstructorResolver constructorResolver = new ExactlyOneConstructorResolver(injectionResolver);
        private final SupplierAdapter supplierAdapter = new Jsr330SupplierAdapter();
        private final ConverterSupplier converterSupplier = to -> Collections.emptyList();
        private final Map<Scope, ProvisionStrategyDecorator> scopeDecorators;
        private final List<ProvisionStrategyDecorator> strategyDecorators;
        private final List<RequestVisitor> requestVisitors = new ArrayList<>();

        public Jsr330(Option ... options) {

            this.options = new HashSet<>();
            Collections.addAll(this.options, options);
            this.options.add(Option.RESTRICT_PLURAL_QUALIFIERS);

            scopeDecorators = new HashMap<>();
            scopeDecorators.put(Scopes.UNDEFINED, ProvisionStrategyDecorator::none);

            Map<Class<? extends Annotation>, Scope> scopeMap = new HashMap<>();
            scopeMap.put(javax.inject.Singleton.class, Scopes.SINGLETON);
            scopeMap.put(null, Scopes.PROTOTYPE);

            metadataResolver =
                    new ConfigurableMetadataResolver(
                            javax.inject.Qualifier.class,
                            Lazy.class,
                            false,
                            javax.inject.Scope.class,
                            scopeMap,
                            this.options.contains(Option.REQUIRE_QUALIFIERS),
                            this.options.contains(Option.RESTRICT_PLURAL_QUALIFIERS));

            strategyDecorators = new ArrayList<>();

        }

        @Override public InjectionResolver injectionResolver() {
            return injectionResolver;
        }

        @Override public QualifierResolver qualifierResolver() {
            return metadataResolver;
        }

        @Override public ResourceMetadataResolver resourceMetadataResolver() {
            return metadataResolver;
        }

        @Override public ConstructorResolver constructorResolver() {
            return constructorResolver;
        }

        @Override public SupplierAdapter supplierAdapter() {
            return supplierAdapter;
        }

        @Override public ConverterSupplier converterSupplier() {
            return converterSupplier;
        }

        @Override public Map<Scope, ProvisionStrategyDecorator> scopeDecorators() {
            return scopeDecorators;
        }

        @Override public List<ProvisionStrategyDecorator> strategyDecorators() {
            return strategyDecorators;
        }

        @Override public Set<Option> options() {
            return options;
        }

        @Override public List<RequestVisitor> requestVisitors() {
            return requestVisitors;
        }

    }
    
}
