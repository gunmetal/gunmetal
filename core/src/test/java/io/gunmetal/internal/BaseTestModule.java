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

    @Provides ProvisionStrategyDecorator strategyDecorator =
            ProvisionStrategyDecorator::none;

    @Provides ProviderAdapter providerAdapter = new GunmetalProviderAdapter();

    @Provides GraphErrors graphErrors = new GraphErrors();

    @Provides GraphLinker graphLinker = new GraphLinker();

    @Provides GraphContext graphContext = new GraphContext(
            strategyDecorator, graphLinker, graphErrors, new HashMap<>());  

    @Provides InjectionResolver injectionResolver = new AnnotationInjectionResolver(Inject.class);

    @Provides ConstructorResolver constructorResolver =
            new ExactlyOneConstructorResolver(injectionResolver);

    @Provides ConfigurableMetadataResolver metadataResolver =
            new ConfigurableMetadataResolver();

    @Provides RequestVisitorFactory requestVisitorFactory =
            new RequestVisitorFactoryImpl(metadataResolver, false);

    @Provides ClassWalker classWalker =
            new ClassWalkerImpl(injectionResolver, false, false);

    @Provides InjectorFactory injectorFactory =
            new InjectorFactoryImpl(metadataResolver, constructorResolver, classWalker);

    @Provides ResourceFactory resourceFactory =
            new ResourceFactoryImpl(injectorFactory, false);

    @Provides BindingFactory bindingFactory = new BindingFactoryImpl(
            resourceFactory, metadataResolver, metadataResolver);

    @Provides DependencyServiceFactory dependencyServiceFactory =
            new DependencyServiceFactoryImpl(bindingFactory, requestVisitorFactory);

    @Provides GraphCache graphCache = new GraphCache(dependencyServiceFactory, null);

    @Provides InternalProvider internalProvider = new GraphProvider(
            providerAdapter, dependencyServiceFactory, to -> Collections.emptyList(), graphCache, graphContext, false);

}
