package io.gunmetal.internal;

import io.gunmetal.spi.Converter;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.ProvisionStrategy;

import java.lang.reflect.Parameter;
import java.util.List;

/**
 * @author rees.byars
 */
interface ResourceAccessorFactory {

    List<ResourceAccessor> createForModule(
            Class<?> module,
            ComponentContext context);

    ResourceAccessor createForParam(
            Parameter parameter,
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
            ResourceAccessor fromAccessor,
            Converter converter,
            Dependency fromDependency,
            Dependency toDependency);

    ResourceAccessor createForReference(
            DependencyRequest referenceRequest,
            ResourceAccessor provisionAccessor,
            Dependency provisionDependency,
            ProvisionStrategy referenceStrategy,
            ReferenceStrategyFactory referenceStrategyFactory,
            ComponentContext componentContext);

    ResourceAccessor createForFalseResource(
            Dependency dependency, ProvisionStrategy provisionStrategy);

}
