package io.gunmetal.internal;

import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.Errors;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author rees.byars
 */
class GraphErrors implements Errors {

    private volatile Map<ComponentMetadata<?>, List<String>> componentErrors;
    private volatile List<String> generalErrors;
    private volatile boolean failFast = false;

    @Override public synchronized Errors add(ComponentMetadata<?> componentMetadata, String errorMessage) {
        if (failFast) {
            throw new RuntimeException(
                    "\n    Errors for " + componentMetadata + " -> " + errorMessage);
        }
        if (componentErrors == null) {
            componentErrors = new HashMap<>();
        }
        List<String> errors = componentErrors.get(componentMetadata);
        if (errors == null) {
            errors = new LinkedList<>();
            componentErrors.put(componentMetadata, errors);
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

        if (componentErrors == null && generalErrors == null) {
            return;
        }

        StringBuilder builder = new StringBuilder("There were errors building the ObjectGraph -> \n");

        final String tab = "    ";

        if (componentErrors != null) {
            for (Map.Entry<ComponentMetadata<?>, List<String>> entry : componentErrors.entrySet()) {
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
            componentErrors = null;
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