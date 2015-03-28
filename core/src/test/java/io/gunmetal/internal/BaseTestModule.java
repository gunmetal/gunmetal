package io.gunmetal.internal;

import io.gunmetal.Inject;
import io.gunmetal.Module;
import io.gunmetal.Provides;
import io.gunmetal.Singleton;
import io.gunmetal.spi.ClassWalker;
import io.gunmetal.spi.ConstructorResolver;
import io.gunmetal.spi.InjectionResolver;
import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.SupplierAdapter;
import io.gunmetal.spi.ProvisionStrategyDecorator;
import io.gunmetal.spi.impl.AnnotationInjectionResolver;
import io.gunmetal.spi.impl.ExactlyOneConstructorResolver;
import io.gunmetal.spi.impl.DefaultSupplierAdapter;

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

    @Provides SupplierAdapter supplierAdapter = new DefaultSupplierAdapter();

    @Provides ComponentErrors componentErrors = new ComponentErrors();

    @Provides ComponentLinker componentLinker = new ComponentLinker();

    @Provides ComponentContext componentContext = new ComponentContext(
            strategyDecorator, componentLinker, componentErrors, new HashMap<>());

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

    @Provides ResourceAccessorFactory resourceAccessorFactory =
            new ResourceAccessorFactoryImpl(bindingFactory, requestVisitorFactory);

    @Provides ComponentRepository componentRepository = new ComponentRepository(resourceAccessorFactory, null);

    @Provides DependencySupplier dependencySupplier = new ComponentDependencySupplier(
            supplierAdapter, resourceAccessorFactory, to -> Collections.emptyList(), componentRepository, componentContext, false);

}
