package io.gunmetal.internal;

import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.Qualifier;
import io.gunmetal.spi.Scope;
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
import java.util.concurrent.ConcurrentHashMap;

/**
* @author rees.byars
*/
class HandlerCache implements LinkableComponentFactory<HandlerCache> {

    private final HandlerCache parentCache;
    private final Map<Dependency<?>, DependencyRequestHandler<?>> requestHandlers = new ConcurrentHashMap<>(64, .75f, 2);
    private final Queue<DependencyRequestHandler<?>> myHandlers = new LinkedList<>();

    HandlerCache(HandlerCache parentCache) {
        this.parentCache = parentCache;
        if (parentCache != null) {
            requestHandlers.putAll(parentCache.requestHandlers);
        }
    }

    void putAll(List<DependencyRequestHandler<?>> requestHandlers) {
        for (DependencyRequestHandler<?> requestHandler : requestHandlers) {
            putAll(requestHandler);
        }
    }

    <T> void putAll(DependencyRequestHandler<T> requestHandler) {
        for (Dependency<? super T> dependency : requestHandler.targets()) {
            put(dependency, requestHandler);
        }
    }

    <T> void put(final Dependency<? super T> dependency, DependencyRequestHandler<T> requestHandler) {
        ComponentMetadata<?> currentComponent = requestHandler.componentMetadata();
        if (currentComponent.isCollectionElement()) {
            putCollectionElement(dependency, requestHandler);
        } else {
            DependencyRequestHandler<?> previous = requestHandlers.get(dependency);
            if (previous != null) {
                ComponentMetadata<?> previousComponent = previous.componentMetadata();
                // TODO better messages.
                // TODO This could randomly pass/fail in case of multiple non-override enabled
                // TODO handlers with a single enabled handler.  Low priority.
                if (previousComponent.isOverrideEnabled() && currentComponent.isOverrideEnabled()) {
                    throw new RuntimeException("more than one of type with override enabled");
                } else if (!previousComponent.isOverrideEnabled() && !currentComponent.isOverrideEnabled()) {
                    throw new RuntimeException("more than one of type without override enabled");
                } else if (currentComponent.isOverrideEnabled()) {
                    requestHandlers.put(dependency, requestHandler);
                    myHandlers.add(requestHandler);
                } // else keep previous
            } else {
                requestHandlers.put(dependency, requestHandler);
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

    @Override public HandlerCache newInstance(Linkers linkers) {

        HandlerCache newCache = new HandlerCache(parentCache);

        myHandlers.forEach(handler -> newCache.putAll(handler.newHandlerInstance(linkers)));

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
            for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
                dependencies.addAll(requestHandler.dependencies());
            }
            return dependencies;
        }

        @Override public DependencyResponse<List<T>> handle(final DependencyRequest<? super List<T>> dependencyRequest) {
            return new DependencyResponse<List<T>>() {
                @Override public ValidatedDependencyResponse<List<T>> validateResponse() {
                    DependencyRequest<T> subRequest =
                            DependencyRequest.create(dependencyRequest, subDependency);
                    for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
                        requestHandler.handle(subRequest).validateResponse();
                    }
                    return new ValidatedDependencyResponse<List<T>>() {
                        @Override public ProvisionStrategy<List<T>> getProvisionStrategy() {
                            return force();
                        }

                        @Override public ValidatedDependencyResponse<List<T>> validateResponse() {
                            return this;
                        }
                    };
                }
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
            return new ComponentMetadata<Class<?>>() {
                @Override public Class<?> provider() {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override public Class<?> providerClass() {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override public ModuleMetadata moduleMetadata() {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override public Qualifier qualifier() {
                    return dependency.qualifier();
                }

                @Override public Scope scope() {
                    return Scopes.PROTOTYPE;
                }

                @Override public boolean isOverrideEnabled() {
                    return false;
                }

                @Override public boolean isCollectionElement() {
                    return false;
                }
            };
        }

        @Override public DependencyRequestHandler<List<T>> newHandlerInstance(Linkers linkers) {
            CollectionRequestHandler<T> newHandler = new CollectionRequestHandler<>(dependency, subDependency);
            for (DependencyRequestHandler<? extends T> requestHandler : requestHandlers) {
                newHandler.requestHandlers.add(requestHandler.newHandlerInstance(linkers));
            }
            return newHandler;
        }

    }

}
