package io.gunmetal.internal;

import io.gunmetal.Provider;
import io.gunmetal.Ref;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProviderAdapter;
import io.gunmetal.spi.ProvisionStrategy;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 *
 * Instances of the provider should not be accessed concurrently.
 *
* @author rees.byars
*/
class GraphProvider implements InternalProvider {

    private final ProviderAdapter providerAdapter;
    private final BindingFactory bindingFactory;
    private final GraphCache graphCache;
    private final GraphContext context;
    private final boolean requireInterfaces;

    GraphProvider(ProviderAdapter providerAdapter,
                  BindingFactory bindingFactory,
                  GraphCache graphCache,
                  GraphContext context,
                  boolean requireInterfaces) {
        this.providerAdapter = providerAdapter;
        this.bindingFactory = bindingFactory;
        this.graphCache = graphCache;
        this.context = context;
        this.requireInterfaces = requireInterfaces;
    }

    @Override public synchronized <T> ProvisionStrategy<? extends T> getProvisionStrategy(
            final DependencyRequest<T> dependencyRequest) {

        ProvisionStrategy<? extends T> strategy = getCachedProvisionStrategy(dependencyRequest);
        if (strategy != null) {
            return strategy;
        }

        Binding<? extends T> binding = bindingFactory.createJitBindingForRequest(dependencyRequest, context);
        if (binding != null) {
            graphCache.put(dependencyRequest.dependency(), binding, context.errors());
            return binding
                    .service(dependencyRequest, context.errors())
                    .provisionStrategy();
        }

        List<Binding<?>> factoryBindingsForRequest =
                bindingFactory.createJitFactoryBindingsForRequest(dependencyRequest, context);
        graphCache.putAll(factoryBindingsForRequest, context.errors());

        strategy = getCachedProvisionStrategy(dependencyRequest);
        if (strategy != null) {
            return strategy;
        }

        context.errors().add(
                dependencyRequest.sourceProvision(),
                "There is no provider defined for a dependency -> " + dependencyRequest.dependency());

        // TODO shouldn't need to cast
        return (p, c) -> { ((GraphErrors) context.errors()).throwIfNotEmpty(); return null; };

    }

    public synchronized <T> ProvisionStrategy<? extends T> getCachedProvisionStrategy(
            final DependencyRequest<T> dependencyRequest) {

        final Dependency<T> dependency = dependencyRequest.dependency();

        if (requireInterfaces &&
                !(dependency.typeKey().raw().isInterface()
                        || dependencyRequest.sourceProvision().overrides().allowNonInterface())) {
            context.errors().add(
                    dependencyRequest.sourceProvision(),
                    "Dependency is not an interface -> " + dependency);
        }
        Binding<? extends T> binding = graphCache.get(dependency);
        if (binding != null) {
            return binding
                    .service(dependencyRequest, context.errors())
                    .provisionStrategy();
        }
        if (providerAdapter.isProvider(dependency)) {
            binding = createReferenceBinding(dependencyRequest, () -> new ProviderStrategyFactory(providerAdapter));
            if (binding != null) {
                graphCache.put(dependency, binding, context.errors());
                return binding
                        .service(dependencyRequest, context.errors())
                        .provisionStrategy();
            }
        }
        if (dependency.typeKey().raw() == Ref.class) {
            binding = createReferenceBinding(dependencyRequest, RefStrategyFactory::new);
            if (binding != null) {
                graphCache.put(dependency, binding, context.errors());
                return binding
                        .service(dependencyRequest, context.errors())
                        .provisionStrategy();
            }
        }
        return null;
    }

    private <T, C> Binding<T> createReferenceBinding(
            final DependencyRequest<T> refRequest, Provider<ReferenceStrategyFactory> factoryProvider) {
        Dependency<?> providerDependency = refRequest.dependency();
        Type providedType = ((ParameterizedType) providerDependency.typeKey().type()).getActualTypeArguments()[0];
        final Dependency<C> provisionDependency = Dependency.from(providerDependency.qualifier(), providedType);
        final Binding<? extends C> provisionBinding = graphCache.get(provisionDependency);
        if (provisionBinding == null) {
            return null;
        }
        ProvisionStrategy<? extends C> provisionStrategy = provisionBinding.force();
        final ProvisionStrategy<T> providerStrategy = factoryProvider.get().create(provisionStrategy, this);
        return new ReferenceBinding<>(
                refRequest,
                providerStrategy,
                factoryProvider.get(),
                provisionBinding,
                provisionDependency);
    }

}
