package io.gunmetal.internal;

import io.gunmetal.spi.Converter;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.ProvisionStrategy;

import java.util.List;

/**
 * @author rees.byars
 */
interface ResourceAccessorFactory {

    List<ResourceAccessor> createForModule(
            Class<?> module,
            ComponentContext context);

    ResourceAccessor createForParam(
            Dependency dependency,
            ComponentContext context);

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
            ReferenceStrategyFactory referenceStrategyFactory,
            ComponentContext componentContext);

    ResourceAccessor createForFalseResource(
            Dependency dependency, ProvisionStrategy provisionStrategy);

}
