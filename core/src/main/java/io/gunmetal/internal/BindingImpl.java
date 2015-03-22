package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;
import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ProvisionErrors;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.RequestVisitor;

import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
class BindingImpl implements Binding {

    private final Resource resource;
    private final List<Dependency> targets;
    private final RequestVisitor moduleRequestVisitor;
    private final AccessFilter<Class<?>> classAccessFilter;
    private final RequestVisitor scopeVisitor;

    BindingImpl(Resource resource,
                List<Dependency> targets,
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

    @Override public List<Dependency> targets() {
        return targets;
    }

    @Override public List<Dependency> dependencies() {
        return resource.dependencies();
    }

    @Override public DependencyResponse service(DependencyRequest dependencyRequest,
                                                Errors errors) {
        DependencyResponseImpl response =
                new DependencyResponseImpl(dependencyRequest, resource.provisionStrategy(), errors);
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

    @Override public ProvisionStrategy force() {
        return resource.provisionStrategy();
    }

    @Override public boolean isModule() {
        return resource.metadata().isModule();
    }

    @Override public boolean isCollectionElement() {
        return resource.metadata().isCollectionElement();
    }

    @Override public boolean allowBindingOverride() {
        return resource.metadata().overrides().allowMappingOverride();
    }

    @Override public Binding replicateWith(GraphContext context) {
        return new BindingImpl(
                resource.replicateWith(context),
                targets,
                moduleRequestVisitor,
                classAccessFilter);
    }

    private static class DependencyResponseImpl implements DependencyResponse, ProvisionErrors {

        List<String> errorMessages;
        final DependencyRequest dependencyRequest;
        final ProvisionStrategy provisionStrategy;
        final Errors errors;

        DependencyResponseImpl(DependencyRequest dependencyRequest,
                               ProvisionStrategy provisionStrategy,
                               Errors errors) {
            this.dependencyRequest = dependencyRequest;
            this.provisionStrategy = provisionStrategy;
            this.errors = errors;
        }

        @Override public ProvisionStrategy provisionStrategy() {
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
