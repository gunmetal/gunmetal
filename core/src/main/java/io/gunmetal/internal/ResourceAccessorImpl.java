package io.gunmetal.internal;

import io.gunmetal.spi.DependencyRequest;
import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ModuleMetadata;
import io.gunmetal.spi.ProvisionErrors;
import io.gunmetal.spi.ProvisionStrategy;
import io.gunmetal.spi.RequestVisitor;
import io.gunmetal.spi.ResourceMetadata;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
class ResourceAccessorImpl implements ResourceAccessor {

    private final Binding binding;
    private final RequestVisitor moduleRequestVisitor;
    private final AccessFilter<Class<?>> classAccessFilter;
    private final RequestVisitor scopeVisitor;

    ResourceAccessorImpl(
            Binding binding,
            RequestVisitor moduleRequestVisitor) {
        this.binding = binding;
        this.moduleRequestVisitor = moduleRequestVisitor;
        this.classAccessFilter = accessFilter(binding.resource());
        scopeVisitor = (dependencyRequest, errors) -> {
            if (!dependencyRequest.sourceProvision().overrides().allowFuzzyScopes() &&
                    !binding.resource().metadata().scope().canInject(dependencyRequest.sourceScope())) {
                errors.add("mis-scoped"); // TODO message, move to request visitor factory
            }
        };
    }

    private ResourceAccessorImpl(
            Binding binding,
            RequestVisitor moduleRequestVisitor,
            AccessFilter<Class<?>> classAccessFilter,
            RequestVisitor scopeVisitor) {
        this.binding = binding;
        this.moduleRequestVisitor = moduleRequestVisitor;
        this.classAccessFilter = classAccessFilter;
        this.scopeVisitor = scopeVisitor;
    }

    @Override public Binding binding() {
        return binding;
    }

    @Override public ProvisionStrategy process(
            DependencyRequest dependencyRequest, Errors errors) {
        RequestErrors response =
                new RequestErrors(dependencyRequest, errors);
        moduleRequestVisitor.visit(dependencyRequest, response);
        scopeVisitor.visit(dependencyRequest, response);
        if (!classAccessFilter.isAccessibleTo(dependencyRequest.sourceModule().moduleClass())) {
            response.add(
                    "The class [" + dependencyRequest.sourceOrigin().getName()
                            + "] does not have access to [" + classAccessFilter.filteredElement() + "]"
            );
        }
        response.complete();
        return force();
    }

    @Override public ProvisionStrategy force() {
        return binding.resource().provisionStrategy();
    }

    @Override public ResourceAccessor replicateWith(ComponentContext context) {
        return new ResourceAccessorImpl(
                binding.replicateWith(context),
                moduleRequestVisitor,
                classAccessFilter,
                scopeVisitor);
    }

    private AccessFilter<Class<?>> accessFilter(Resource resource) {

        ResourceMetadata<?> resourceMetadata = resource.metadata();
        ModuleMetadata moduleMetadata = resourceMetadata.moduleMetadata();

        // TODO this could be better
        AccessFilter<Class<?>> accessFilter;
        if (resourceMetadata.provider() instanceof Class) {
            accessFilter = AccessFilter.create(
                    (Class<?>) resourceMetadata.provider());
        } else if (resourceMetadata.provider() instanceof Member) {
            accessFilter = AccessFilter.create(
                    (AnnotatedElement & Member) resourceMetadata.provider());
        } else {
            accessFilter = AccessFilter.create(resourceMetadata.providerClass());
        }

        return new AccessFilter<Class<?>>() {
            @Override public AnnotatedElement filteredElement() {
                return accessFilter.filteredElement();
            }
            @Override public boolean isAccessibleTo(Class<?> target) {
                // supports library access
                return target == moduleMetadata.moduleClass() || accessFilter.isAccessibleTo(target);
            }
        };

    }

    private static class RequestErrors implements ProvisionErrors {

        List<String> errorMessages;
        final DependencyRequest dependencyRequest;
        final Errors errors;

        RequestErrors(DependencyRequest dependencyRequest,
                      Errors errors) {
            this.dependencyRequest = dependencyRequest;
            this.errors = errors;
        }

        @Override public void add(String errorMessage) {
            if (errorMessages == null) {
                errorMessages = new LinkedList<>();
            }
            errorMessages.add(errorMessage);
        }

        void complete() {
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
