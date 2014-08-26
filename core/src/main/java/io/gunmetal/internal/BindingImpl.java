package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ProvisionErrors;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.RequestVisitor;
import io.gunmetal.spi.ResourceMetadata;

import java.util.LinkedList;
import java.util.List;

/**
* @author rees.byars
*/
class BindingImpl<T> implements Binding<T> {

    private final Resource<T> resource;
    private final List<Dependency<? super T>> targets;
    private final RequestVisitor moduleRequestVisitor;
    private final AccessFilter<Class<?>> classAccessFilter;
    private final RequestVisitor scopeVisitor;

    BindingImpl(Resource<T> resource,
                List<Dependency<? super T>> targets,
                RequestVisitor moduleRequestVisitor,
                AccessFilter<Class<?>> classAccessFilter) {
        this.resource = resource;
        this.targets = targets;
        this.moduleRequestVisitor = moduleRequestVisitor;
        this.classAccessFilter = classAccessFilter;
        scopeVisitor = (dependencyRequest, errors) -> {
            if (!resource.metadata().scope().canInject(dependencyRequest.sourceScope())) {
                errors.add("mis-scoped"); // TODO message
            }
        };
    }

    @Override public List<Dependency<? super T>> targets() {
        return targets;
    }

    @Override public List<Dependency<?>> dependencies() {
        return resource.dependencies();
    }

    @Override public DependencyResponse<T> service(DependencyRequest<? super T> dependencyRequest,
                                                   Errors errors) {
        DependencyResponseImpl<T> response =
                new DependencyResponseImpl<>(dependencyRequest, resource.provisionStrategy(), errors);
        moduleRequestVisitor.visit(dependencyRequest, response);
        scopeVisitor.visit(dependencyRequest, response);
        if (!classAccessFilter.isAccessibleTo(dependencyRequest.sourceModule().moduleClass())) {
            response.add(
                    "The class [" + dependencyRequest.sourceOrigin().getName()
                    + "] does not have access to [" + classAccessFilter.filteredElement() + "]"
            );
        }
        response.validateResponse();
        return response;
    }

    @Override public ProvisionStrategy<T> force() {
        return resource.provisionStrategy();
    }

    @Override public ResourceMetadata<?> resourceMetadata() {
        return resource.metadata();
    }

    @Override public Binding<T> replicateWith(GraphContext context) {
        return new BindingImpl<>(
                resource.replicateWith(context),
                targets,
                moduleRequestVisitor,
                classAccessFilter);
    }

    private static class DependencyResponseImpl<T> implements DependencyResponse<T>, ProvisionErrors {

        List<String> errorMessages;
        final DependencyRequest<? super T> dependencyRequest;
        final ProvisionStrategy<? extends T> provisionStrategy;
        final Errors errors;

        DependencyResponseImpl(DependencyRequest<? super T> dependencyRequest,
                               ProvisionStrategy<T> provisionStrategy,
                               Errors errors) {
            this.dependencyRequest = dependencyRequest;
            this.provisionStrategy = provisionStrategy;
            this.errors = errors;
        }

        @Override public ProvisionStrategy<? extends T> provisionStrategy() {
            return provisionStrategy;
        }

        @Override public void add(String errorMessage) {
            if (errorMessages == null) {
                errorMessages = new LinkedList<>();
            }
            errorMessages.add(errorMessage);
        }

        void validateResponse() {
            if (errorMessages != null) {
                for (String error : errorMessages) {
                    errors.add(
                            dependencyRequest.sourceProvision(),
                            "Denied request for " + dependencyRequest.dependency() + ".  Reason -> " + error);
                }
            }
        }
    }

}
