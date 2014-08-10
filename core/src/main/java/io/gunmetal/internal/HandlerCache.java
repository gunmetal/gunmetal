package io.gunmetal.internal;

import io.gunmetal.Overrides;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.Scopes;
import io.gunmetal.util.Generics;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
* @author rees.byars
*/
class HandlerCache implements Replicable<HandlerCache> {

    private final HandlerCache parentCache;
    private final Map<Dependency<?>, DependencyRequestHandler<?>> requestHandlers = new ConcurrentHashMap<>(64, .75f, 2);
    private final Set<Dependency<?>> overriddenDependencies = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Queue<DependencyRequestHandler<?>> myHandlers = new LinkedList<>();

    HandlerCache(HandlerCache parentCache) {
        this.parentCache = parentCache;
        if (parentCache != null) {
            requestHandlers.putAll(parentCache.requestHandlers);
        }
    }

    void putAll(List<DependencyRequestHandler<?>> requestHandlers, Errors errors) {
        for (DependencyRequestHandler<?> requestHandler : requestHandlers) {
            putAll(requestHandler, errors);
        }
    }

    <T> void putAll(DependencyRequestHandler<T> requestHandler, Errors errors) {
        for (Dependency<? super T> dependency : requestHandler.targets()) {
            put(dependency, requestHandler, errors);
        }
    }

    <T> void put(final Dependency<? super T> dependency, DependencyRequestHandler<T> requestHandler, Errors errors) {
        ComponentMetadata<?> currentComponent = requestHandler.componentMetadata();
        if (currentComponent.isCollectionElement()) {
            putCollectionElement(dependency, requestHandler);
        } else {
            DependencyRequestHandler<?> previous = requestHandlers.put(dependency, requestHandler);
            if (previous != null) {
                ComponentMetadata<?> previousComponent = previous.componentMetadata();
                // TODO better messages, include components, keep list?
                if (previousComponent.overrides().allowMappingOverride()
                        && currentComponent.overrides().allowMappingOverride()) {
                    errors.add("more than one of type with override enabled");
                    requestHandlers.put(dependency, previous);
                } else if (
                        (overriddenDependencies.contains(dependency)
                                && !currentComponent.overrides().allowMappingOverride())
                        || (!previousComponent.overrides().allowMappingOverride()
                                && !currentComponent.overrides().allowMappingOverride())) {
                    errors.add("more than one of type without override enabled");
                    requestHandlers.put(dependency, previous);
                } else if (currentComponent.overrides().allowMappingOverride()) {
                    myHandlers.add(requestHandler);
                    overriddenDependencies.add(dependency);
                } else if (previousComponent.overrides().allowMappingOverride()) {
                    requestHandlers.put(dependency, previous);
                    overriddenDependencies.add(dependency);
                }
            } else {
                myHandlers.add(requestHandler);
            }
        }
    }

    <T> DependencyRequestHandler<? extends T> get(Dependency<T> dependency) {
        return Generics.as(requestHandlers.get(dependency));
    }

    private <T> void putCollectionElement(final Dependency<T> dependency,
                                  DependencyRequestHandler<? extends T> requestHandler) {
        Dependency<List<T>> collectionDependency =
                Dependency.from(dependency.qualifier(), new ParameterizedType() {
                    @Override public Type[] getActualTypeArguments() {
                        return new Type[] {dependency.typeKey().type()};
                    }

                    @Override public Type getRawType() {
                        return List.class;
                    }

                    @Override public Type getOwnerType() {
                        return null;
                    }

                    @Override public int hashCode() {
                        return Arrays.hashCode(getActualTypeArguments()) * 67 + getRawType().hashCode();
                    }

                    @Override public boolean equals(Object target) {
                        if (target == this) {
                            return true;
                        }
                        if (!(target instanceof ParameterizedType)) {
                            return false;
                        }
                        ParameterizedType parameterizedType = (ParameterizedType) target;
                        return parameterizedType.getRawType().equals(getRawType())
                                && Arrays.equals(parameterizedType.getActualTypeArguments(), getActualTypeArguments());
                    }

                });
        CollectionRequestHandler<T> collectionRequestHandler
                = Generics.as(requestHandlers.get(collectionDependency));
        if (collectionRequestHandler == null) {
            collectionRequestHandler = new CollectionRequestHandler<>(collectionDependency, dependency);
            requestHandlers.put(collectionDependency, collectionRequestHandler);
            myHandlers.add(collectionRequestHandler);
        }
        collectionRequestHandler.requestHandlers.add(requestHandler);
    }

    @Override public HandlerCache replicateWith(GraphContext context) {

        HandlerCache newCache = new HandlerCache(parentCache);

        myHandlers.forEach(handler -> newCache.putAll(handler.replicateWith(context), context.errors()));

        return newCache;

    }

    private static class CollectionRequestHandler<T> implements DependencyRequestHandler<List<T>> {

        private final List<DependencyRequestHandler<? extends T>> requestHandlers = new ArrayList<>();
        private final Dependency<List<T>> dependency;
        private final Dependency<T> subDependency;

        CollectionRequestHandler(Dependency<List<T>> dependency, Dependency<T> subDependency) {
            this.dependency = dependency;
            this.subDependency = subDependency;
        }

        @Override public List<Dependency<? super List<T>>> targets() {
            return Collections.<Dependency<? super List<T>>>singletonList(dependency);
        }

        @Override public List<Dependency<?>> dependencies() {
            List<Dependency<?>> dependencies = new LinkedList<>();
            requestHandlers.stream().forEach(handler -> dependencies.addAll(handler.dependencies()));
            return dependencies;
        }

        @Override public DependencyResponse<List<T>> handle(final DependencyRequest<? super List<T>> dependencyRequest) {
            return () -> {
                DependencyRequest<T> subRequest =
                        DependencyRequest.create(dependencyRequest, subDependency);
                for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
                    requestHandler.handle(subRequest).validateResponse();
                }
                return new DependencyResponse.ValidatedDependencyResponse<List<T>>() {
                    @Override public ProvisionStrategy<List<T>> getProvisionStrategy() {
                        return force();
                    }

                    @Override public ValidatedDependencyResponse<List<T>> validateResponse() {
                        return this;
                    }
                };
            };
        }

        @Override public ProvisionStrategy<List<T>> force() {
            return (internalProvider, resolutionContext) -> {
                List<T> list = new LinkedList<>();
                for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
                    ProvisionStrategy<? extends T> provisionStrategy = requestHandler.force();
                    list.add(provisionStrategy.get(internalProvider, resolutionContext));
                }
                return list;
            };
        }

        @Override public ComponentMetadata<?> componentMetadata() {
            return new ComponentMetadata<Class<?>>(
                    null,
                    null,
                    null,
                    dependency.qualifier(),
                    Scopes.PROTOTYPE,
                    Overrides.NONE,
                    false,
                    false,
                    false,
                    false,
                    true) {
                @Override public Class<?> provider() {
                    throw new UnsupportedOperationException(); // TODO
                }
                @Override public Class<?> providerClass() {
                    throw new UnsupportedOperationException(); // TODO
                }
                @Override public ModuleMetadata moduleMetadata() {
                    throw new UnsupportedOperationException(); // TODO
                }
            };
        }

        @Override public DependencyRequestHandler<List<T>> replicateWith(GraphContext context) {
            CollectionRequestHandler<T> newHandler = new CollectionRequestHandler<>(dependency, subDependency);
            for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
                newHandler.requestHandlers.add(requestHandler.replicateWith(context));
            }
            return newHandler;
        }

    }

}
