package io.gunmetal.internal;

import io.gunmetal.spi.Errors;
import io.gunmetal.spi.ResourceMetadata;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author rees.byars
 */
class GraphErrors implements Errors {

    private volatile Map<ResourceMetadata<?>, List<String>> provisionErrors;
    private volatile List<String> generalErrors;
    private volatile boolean failFast = false;

    GraphErrors() {
    }

    @Override public synchronized Errors add(ResourceMetadata<?> resourceMetadata, String errorMessage) {
        if (failFast) {
            throw new RuntimeException(
                    "\n    Errors for " + resourceMetadata + " -> " + errorMessage);
        }
        if (provisionErrors == null) {
            provisionErrors = new HashMap<>();
        }
        List<String> errors = provisionErrors.get(resourceMetadata);
        if (errors == null) {
            errors = new LinkedList<>();
            provisionErrors.put(resourceMetadata, errors);
        }
        errors.add(errorMessage);
        return this;
    }

    @Override public synchronized Errors add(String errorMessage) {
        if (failFast) {
            throw new RuntimeException(errorMessage);
        }
        if (generalErrors == null) {
            generalErrors = new LinkedList<>();
        }
        generalErrors.add(errorMessage);
        return this;
    }

    synchronized void throwIfNotEmpty() {

        failFast = true;

        if (provisionErrors == null && generalErrors == null) {
            return;
        }

        StringBuilder builder = new StringBuilder("There were errors building the ObjectGraph -> \n");

        final String tab = "    ";

        if (provisionErrors != null) {
            for (Map.Entry<ResourceMetadata<?>, List<String>> entry : provisionErrors.entrySet()) {
                builder
                        .append("\n")
                        .append(tab)
                        .append("Errors for ")
                        .append(entry.getKey())
                        .append(" -> ");
                for (String errorMessage : entry.getValue()) {
                    builder.append("\n").append(tab).append(tab).append(errorMessage);
                }
            }
            provisionErrors = null;
        }

        if (generalErrors != null) {
            for (String errorMessage : generalErrors) {
                builder.append("\n").append(tab).append("graph error -> ").append(errorMessage);
            }
            generalErrors = null;
        }

        throw new RuntimeException(builder.toString());

    }

}
