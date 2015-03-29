package io.gunmetal.internal;

import io.gunmetal.Inject;
import io.gunmetal.Module;
import io.gunmetal.Supplies;
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
@Module(type = Module.Type.CONSTRUCTED)
public class BaseTestModule {

    @Supplies ProvisionStrategyDecorator strategyDecorator =
            ProvisionStrategyDecorator::none;

    @Supplies SupplierAdapter supplierAdapter = new DefaultSupplierAdapter();

    @Supplies ComponentErrors componentErrors = new ComponentErrors();

    @Supplies ComponentLinker componentLinker = new ComponentLinker();

    @Supplies ComponentContext componentContext = new ComponentContext(
            strategyDecorator, componentLinker, componentErrors, new HashMap<>());

    @Supplies InjectionResolver injectionResolver = new AnnotationInjectionResolver(Inject.class);

    @Supplies ConstructorResolver constructorResolver =
            new ExactlyOneConstructorResolver(injectionResolver);

    @Supplies ConfigurableMetadataResolver metadataResolver =
            new ConfigurableMetadataResolver();

    @Supplies RequestVisitorFactory requestVisitorFactory =
            new RequestVisitorFactoryImpl(metadataResolver, false);

    @Supplies ClassWalker classWalker =
            new ClassWalkerImpl(injectionResolver, false, false);

    @Supplies InjectorFactory injectorFactory =
            new InjectorFactoryImpl(metadataResolver, constructorResolver, classWalker);

    @Supplies ResourceFactory resourceFactory =
            new ResourceFactoryImpl(injectorFactory, false);

    @Supplies BindingFactory bindingFactory = new BindingFactoryImpl(
            resourceFactory, metadataResolver, metadataResolver);

    @Supplies ResourceAccessorFactory resourceAccessorFactory =
            new ResourceAccessorFactoryImpl(bindingFactory, requestVisitorFactory);

    @Supplies ComponentRepository componentRepository = new ComponentRepository(resourceAccessorFactory, null);

    @Supplies DependencySupplier dependencySupplier = new ComponentDependencySupplier(
            supplierAdapter, resourceAccessorFactory, to -> Collections.emptyList(), componentRepository, componentContext, false);

}
