package io.gunmetal.internal;

import io.gunmetal.Inject;
import io.gunmetal.Module;
import io.gunmetal.Provides;
import io.gunmetal.Singleton;
import io.gunmetal.spi.ClassWalker;
import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.spi.InjectionResolver;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProviderAdapter;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.QualifierResolver;
import io.gunmetal.spi.ResourceMetadataResolver;
import io.gunmetal.spi.impl.AnnotationInjectionResolver;
import io.gunmetal.spi.impl.ExactlyOneConstructorResolver;
import io.gunmetal.spi.impl.GunmetalProviderAdapter;

import java.util.Collections;
import java.util.HashMap;

/**
 * @author rees.byars
 */
@Singleton
@Module(stateful = true, provided = false)
public class BaseTestModule {

    @Provides @Singleton BindingFactory bindingFactory(ResourceFactory resourceFactory,
                                                       QualifierResolver qualifierResolver,
                                                       ResourceMetadataResolver metadataResolver,
                                                       RequestVisitorFactory visitorFactory) {
        return new BindingFactoryImpl(resourceFactory, qualifierResolver, metadataResolver, visitorFactory);
    }

    @Provides @Singleton ResourceFactory resourceFactory(InjectorFactory injectorFactory) {
        return new ResourceFactoryImpl(injectorFactory, false);
    }

    @Provides @Singleton InjectorFactory injectorFactory(QualifierResolver qualifierResolver,
                                                         ConstructorResolver constructorResolver,
                                                         ClassWalker classWalker) {
        return new InjectorFactoryImpl(qualifierResolver, constructorResolver, classWalker);
    }

    @Provides @Singleton ConfigurableMetadataResolver configurableMetadataResolver() {
        return new ConfigurableMetadataResolver();
    }

    @Provides @Singleton QualifierResolver qualifierResolver(ConfigurableMetadataResolver metadataResolver) {
        return metadataResolver;
    }

    @Provides @Singleton ResourceMetadataResolver resourceMetadataResolver(ConfigurableMetadataResolver metadataResolver) {
        return metadataResolver;
    }

    @Provides @Singleton ClassWalker classWalker(InjectionResolver injectionResolver) {
        return new ClassWalkerImpl(injectionResolver, false, false);
    }

    @Provides @Singleton RequestVisitorFactory requestVisitorFactory(QualifierResolver qualifierResolver) {
        return new RequestVisitorFactoryImpl(qualifierResolver, false);
    }

    @Provides @Singleton ConstructorResolver constructorResolver(InjectionResolver injectionResolver) {
        return new ExactlyOneConstructorResolver(injectionResolver);
    }

    @Provides @Singleton InjectionResolver injectionResolver() {
        return new AnnotationInjectionResolver(Inject.class);
    }

    @Provides @Singleton GraphContext graphContext(ProvisionStrategyDecorator provisionStrategyDecorator,
                                                   GraphLinker graphLinker,
                                                   GraphErrors graphErrors) {
        return new GraphContext(
                provisionStrategyDecorator,
                graphLinker,
                graphErrors,
                new HashMap<>()
        );
    }

    @Provides @Singleton GraphErrors graphErrors() {
        return new GraphErrors();
    }

    @Provides @Singleton GraphLinker graphLinker() {
        return new GraphLinker();
    }

    @Provides @Singleton ProvisionStrategyDecorator provisionStrategyDecorator() {
        return ProvisionStrategyDecorator::none;
    }

    @Provides @Singleton InternalProvider internalProvider(ProviderAdapter providerAdapter,
                                                           BindingFactory bindingFactory,
                                                           GraphCache graphCache,
                                                           GraphContext graphContext) {
        return new GraphProvider(
                providerAdapter,
                bindingFactory,
                to -> Collections.emptyList(),
                graphCache,
                graphContext,
                false);
    }

    @Provides @Singleton ProviderAdapter providerAdapter() {
        return new GunmetalProviderAdapter();
    }

    @Provides @Singleton GraphCache graphCache() {
        return new GraphCache(null);
    }

}
