package io.gunmetal.internal;

import io.gunmetal.spi.Converter;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.ProvisionStrategy;

import java.util.List;
import java.util.Set;

/**
 * @author rees.byars
 */
interface DependencyServiceFactory {

    List<DependencyService> createForModule(
            Class<?> module,
            ComponentContext context,
            Set<Class<?>> loadedModules);

    DependencyService createJit(
            DependencyRequest dependencyRequest,
            ComponentContext context);

    List<DependencyService> createJitFactoryRequest(
            DependencyRequest dependencyRequest,
            ComponentContext context);

    CollectionDependencyService createForCollection(
            Dependency collectionDependency,
            Dependency collectionElementDependency);

    DependencyService createForConversion(
            DependencyService fromService,
            Converter converter,
            Dependency fromDependency,
            Dependency toDependency);

    DependencyService createForReference(
            DependencyRequest referenceRequest,
            DependencyService provisionService,
            Dependency provisionDependency,
            ProvisionStrategy referenceStrategy,
            ReferenceStrategyFactory referenceStrategyFactory);

    DependencyService createForFalseResource(
            Dependency dependency, ProvisionStrategy provisionStrategy);

}
