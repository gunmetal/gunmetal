package io.gunmetal.internal;

import io.gunmetal.Module;
import io.gunmetal.Overrides;
import io.gunmetal.Provider;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.Scopes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
* @author rees.byars
*/
class CollectionRequestHandler<T> implements DependencyRequestHandler<Collection<T>> {

    private final List<DependencyRequestHandler<? extends T>> requestHandlers = new ArrayList<>();
    private final Dependency<Collection<T>> dependency;
    private final Dependency<T> subDependency;
    private final Provider<Collection<T>> collectionProvider;

    CollectionRequestHandler(Dependency<Collection<T>> dependency,
                             Dependency<T> subDependency,
                             Provider<Collection<T>> collectionProvider) {
        this.dependency = dependency;
        this.subDependency = subDependency;
        this.collectionProvider = collectionProvider;
    }

    @Override public List<Dependency<? super Collection<T>>> targets() {
        return Collections.<Dependency<? super Collection<T>>>singletonList(dependency);
    }

    @Override public List<Dependency<?>> dependencies() {
        List<Dependency<?>> dependencies = new LinkedList<>();
        requestHandlers.stream().forEach(handler -> dependencies.addAll(handler.dependencies()));
        return dependencies;
    }

    @Override public DependencyResponse<Collection<T>> handle(
            final DependencyRequest<? super Collection<T>> dependencyRequest) {
        return () -> {
            DependencyRequest<T> subRequest =
                    DependencyRequest.create(dependencyRequest, subDependency);
            for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
                requestHandler.handle(subRequest);
            }
            return force();
        };
    }

    @Override public ProvisionStrategy<Collection<T>> force() {
        return (internalProvider, resolutionContext) -> {
            Collection<T> collection = collectionProvider.get();
            for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
                ProvisionStrategy<? extends T> provisionStrategy = requestHandler.force();
                collection.add(provisionStrategy.get(internalProvider, resolutionContext));
            }
            return collection;
        };
    }

    @Override public ProvisionMetadata<?> provisionMetadata() {
        return new ProvisionMetadata<Class<?>>(
                CollectionRequestHandler.class,
                CollectionRequestHandler.class,
                new ModuleMetadata(CollectionRequestHandler.class, Qualifier.NONE, Module.NONE),
                dependency.qualifier(),
                Scopes.PROTOTYPE,
                Overrides.NONE,
                false,
                false,
                false,
                false,
                true);
    }

    @Override public DependencyRequestHandler<Collection<T>> replicateWith(GraphContext context) {
        CollectionRequestHandler<T> newHandler =
                new CollectionRequestHandler<>(dependency, subDependency, collectionProvider);
        for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
            newHandler.requestHandlers.add(requestHandler.replicateWith(context));
        }
        return newHandler;
    }

    void add(DependencyRequestHandler<? extends T> subHandler) {
        requestHandlers.add(subHandler);
    }

}
