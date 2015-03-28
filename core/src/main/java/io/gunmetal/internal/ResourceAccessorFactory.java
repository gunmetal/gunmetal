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
interface ResourceAccessorFactory {

    List<ResourceAccessor> createForModule(
            Class<?> module,
            ComponentContext context,
            Set<Class<?>> loadedModules);

    ResourceAccessor createJit(
            DependencyRequest dependencyRequest,
            ComponentContext context);

    List<ResourceAccessor> createJitFactoryRequest(
            DependencyRequest dependencyRequest,
            ComponentContext context);

    CollectionResourceAccessor createForCollection(
            Dependency collectionDependency,
            Dependency collectionElementDependency);

    ResourceAccessor createForConversion(
            ResourceAccessor fromService,
            Converter converter,
            Dependency fromDependency,
            Dependency toDependency);

    ResourceAccessor createForReference(
            DependencyRequest referenceRequest,
            ResourceAccessor provisionService,
            Dependency provisionDependency,
            ProvisionStrategy referenceStrategy,
            ReferenceStrategyFactory referenceStrategyFactory);

    ResourceAccessor createForFalseResource(
            Dependency dependency, ProvisionStrategy provisionStrategy);

}
