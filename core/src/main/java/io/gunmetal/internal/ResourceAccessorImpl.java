package io.gunmetal.internal;

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
class ResourceAccessorImpl implements ResourceAccessor {

    private final Binding binding;
    private final RequestVisitor requestVisitor;

    ResourceAccessorImpl(
            Binding binding,
            RequestVisitor requestVisitor) {
        this.binding = binding;
        this.requestVisitor = requestVisitor;
    }

    @Override public Binding binding() {
        return binding;
    }

    @Override public ProvisionStrategy process(
            DependencyRequest dependencyRequest, Errors errors) {
        RequestErrors requestErrors = new RequestErrors(dependencyRequest, errors);
        requestVisitor.visit(dependencyRequest, requestErrors);
        requestErrors.complete();
        return force();
    }

    @Override public ProvisionStrategy force() {
        return binding.resource().provisionStrategy();
    }

    @Override public ResourceAccessor replicateWith(ComponentContext context) {
        return new ResourceAccessorImpl(
                binding.replicateWith(context),
                requestVisitor);
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
