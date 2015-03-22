package io.gunmetal.internal;

import io.gunmetal.Provider;
import io.gunmetal.Ref;
import io.gunmetal.spi.Converter;
import io.gunmetal.spi.ConverterProvider;
import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.ProviderAdapter;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.TypeKey;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Instances of the provider should not be accessed concurrently.
 *
 * @author rees.byars
 */
class GraphProvider implements InternalProvider {

    private final ProviderAdapter providerAdapter;
    private final BindingFactory bindingFactory;
    private final ConverterProvider converterProvider;
    private final GraphCache graphCache;
    private final GraphContext context;
    private final boolean requireInterfaces;

    GraphProvider(ProviderAdapter providerAdapter,
                  BindingFactory bindingFactory,
                  ConverterProvider converterProvider,
                  GraphCache graphCache,
                  GraphContext context,
                  boolean requireInterfaces) {
        this.providerAdapter = providerAdapter;
        this.bindingFactory = bindingFactory;
        this.converterProvider = converterProvider;
        this.graphCache = graphCache;
        this.context = context;
        this.requireInterfaces = requireInterfaces;
    }

    @Override public synchronized ProvisionStrategy getProvisionStrategy(
            final DependencyRequest dependencyRequest) {

        // try cached strategy
        ProvisionStrategy strategy = getCachedProvisionStrategy(dependencyRequest);
        if (strategy != null) {
            return strategy;
        }

        // try jit constructor binding strategy
        Binding binding = bindingFactory.createJitBindingForRequest(dependencyRequest, context);
        if (binding != null) {
            graphCache.put(dependencyRequest.dependency(), binding, context.errors());
            return binding
                    .service(dependencyRequest, context.errors())
                    .provisionStrategy();
        }

        // try conversion strategy
        Dependency dependency = dependencyRequest.dependency();
        TypeKey typeKey = dependency.typeKey();
        for (Converter converter : converterProvider.convertersForType(typeKey)) {
            for (Class<?> fromType : converter.supportedFromTypes()) {
                binding = createConversionBinding(converter, fromType, dependency);
                if (binding != null) {
                    graphCache.put(dependency, binding, context.errors());
                    return binding
                            .service(dependencyRequest, context.errors())
                            .provisionStrategy();
                }
            }
        }

        // try jit local factory method binding
        List<Binding> factoryBindingsForRequest =
                bindingFactory.createJitFactoryBindingsForRequest(dependencyRequest, context);
        graphCache.putAll(factoryBindingsForRequest, context.errors());
        strategy = getCachedProvisionStrategy(dependencyRequest);
        if (strategy != null) {
            return strategy;
        }

        // all attempts to serve request have failed
        context.errors().add(
                dependencyRequest.sourceProvision(),
                "There is no provider defined for a dependency -> " + dependencyRequest.dependency());

        // TODO shouldn't need to cast
        return (p, c) -> {
            ((GraphErrors) context.errors()).throwIfNotEmpty();
            return null;
        };

    }

    public synchronized ProvisionStrategy getCachedProvisionStrategy(final DependencyRequest dependencyRequest) {

        final Dependency dependency = dependencyRequest.dependency();

        if (requireInterfaces &&
                !(dependency.typeKey().raw().isInterface()
                        || dependencyRequest.sourceProvision().overrides().allowNonInterface())) {
            context.errors().add(
                    dependencyRequest.sourceProvision(),
                    "Dependency is not an interface -> " + dependency);
        }
        Binding binding = graphCache.get(dependency);
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

    private Binding createReferenceBinding(
            final DependencyRequest refRequest, Provider<ReferenceStrategyFactory> factoryProvider) {
        Dependency providerDependency = refRequest.dependency();
        Type providedType = ((ParameterizedType) providerDependency.typeKey().type()).getActualTypeArguments()[0];
        final Dependency provisionDependency = Dependency.from(providerDependency.qualifier(), providedType);
        final Binding provisionBinding = graphCache.get(provisionDependency);
        if (provisionBinding == null) {
            return null;
        }
        ProvisionStrategy provisionStrategy = provisionBinding.force();
        final ProvisionStrategy providerStrategy = factoryProvider.get().create(provisionStrategy, this);
        return new ReferenceBinding(
                refRequest,
                providerStrategy,
                factoryProvider.get(),
                provisionBinding,
                provisionDependency);
    }

    private Binding createConversionBinding(
            Converter converter, Class<?> fromType, Dependency to) {
        Dependency from = Dependency.from(to.qualifier(), fromType);
        Binding fromBinding = graphCache.get(from);
        if (fromBinding != null) {
            return new ConversionBinding(fromBinding, converter, from, to);
        }
        return null;
    }

}
