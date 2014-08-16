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

package io.gunmetal.spi;

import io.gunmetal.Overrides;

import java.lang.reflect.AnnotatedElement;

/**
 * @author rees.byars
 */
public class ProvisionMetadata<P extends AnnotatedElement> {

    private final P provider;
    private final Class<?> providerClass;
    private final ModuleMetadata moduleMetadata;
    private final Qualifier qualifier;
    private final Scope scope;
    private final Overrides overrides;
    private final boolean eager;
    private final boolean isCollectionElement;
    private final boolean isModule;
    private final boolean isProvided;
    private final boolean isProvider;
    private final int hashCode;
    
    public ProvisionMetadata(P provider,
                             Class<?> providerClass,
                             ModuleMetadata moduleMetadata,
                             Qualifier qualifier,
                             Scope scope,
                             Overrides overrides,
                             boolean eager,
                             boolean isCollectionElement,
                             boolean isModule,
                             boolean isProvided,
                             boolean isProvider) {
        this.provider = provider;
        this.providerClass = providerClass;
        this.moduleMetadata = moduleMetadata;
        this.qualifier = qualifier;
        this.scope = scope;
        this.overrides = overrides;
        this.eager = eager;
        this.isCollectionElement = isCollectionElement;
        this.isModule = isModule;
        this.isProvided = isProvided;
        this.isProvider = isProvider;
        hashCode = provider().hashCode() * 67 + qualifier().hashCode();
    }

    public P provider() {
        return provider;
    }

    public Class<?> providerClass() {
        return providerClass;
    }

    public ModuleMetadata moduleMetadata() {
        return moduleMetadata;
    }

    public Qualifier qualifier() {
        return qualifier;
    }

    public Scope scope() {
        return scope;
    }

    public Overrides overrides() {
        return overrides;
    }

    public boolean eager() {
        return eager;
    }

    public boolean isCollectionElement() {
        return isCollectionElement;
    }

    public boolean isModule() {
        return isModule;
    }

    public boolean isProvided() {
        return isProvided;
    }

    public boolean isProvider() {
        return isProvider;
    }

    @Override public final int hashCode() {
        return hashCode;
    }

    @Override public final boolean equals(Object target) {
        if (target == this) {
            return true;
        }
        if (!(target instanceof ProvisionMetadata<?>)) {
            return false;
        }
        ProvisionMetadata<?> provisionMetadataTarget = (ProvisionMetadata<?>) target;
        return provisionMetadataTarget.qualifier().equals(qualifier())
                && provisionMetadataTarget.provider().equals(provider());
    }

    @Override public final String toString() {
        return "provision[ " + qualifier() + ", provider[ " + provider() + "] ]";
    }

}
