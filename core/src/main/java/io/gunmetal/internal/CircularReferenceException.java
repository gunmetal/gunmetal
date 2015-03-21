/*
 * Copyright (c) 2013.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gunmetal.internal;

import io.gunmetal.spi.ResourceMetadata;
import io.gunmetal.spi.ProvisionStrategy;

import java.util.Stack;

/**
 * @author rees.byars
 */
class CircularReferenceException extends RuntimeException {

    private static final long serialVersionUID = -7837281223529967792L;
    private final ResourceMetadata<?> metadata;
    private ProvisionStrategy<?> reverseStrategy;
    private final Stack<ResourceMetadata<?>> resourceMetadataStack = new Stack<>();

    CircularReferenceException(ResourceMetadata<?> metadata) {
        this.metadata = metadata;
    }

    public ResourceMetadata<?> metadata() {
        return metadata;
    }

    public void setReverseStrategy(ProvisionStrategy<?> reverseStrategy) {
        this.reverseStrategy = reverseStrategy;
    }

    public ProvisionStrategy<?> getReverseStrategy() {
        return reverseStrategy;
    }

    public void push(ResourceMetadata<?> resourceMetadata) {
        resourceMetadataStack.push(resourceMetadata);
    }

    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder("Circular dependency detected -> \n");
        builder.append("    ").append(metadata);
        while (!resourceMetadataStack.empty()) {
            builder.append("\n     was requested by ");
            builder.append(resourceMetadataStack.pop());
        }
        return builder.toString();
    }

}
