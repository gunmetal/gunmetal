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

import java.lang.reflect.AnnotatedElement;

/**
 * @author rees.byars
 */
public abstract class ComponentMetadata<P extends AnnotatedElement> {

    public abstract P provider();

    public abstract Class<?> providerClass();

    public abstract ModuleMetadata moduleMetadata();

    public abstract Qualifier qualifier();

    public abstract Scope scope();

    // abstract boolean isProxy();  TODO scoped proxy invoke(..) { method.invoke(provider.get(), ...); ... }

    @Override public int hashCode() {
        return provider().hashCode() * 67 + qualifier().hashCode();
    }

    @Override public boolean equals(Object target) {
        if (target == this) {
            return true;
        }
        if (!(target instanceof ComponentMetadata<?>)) {
            return false;
        }
        ComponentMetadata<?> componentMetadataTarget = (ComponentMetadata<?>) target;
        return componentMetadataTarget.qualifier().equals(qualifier())
                && componentMetadataTarget.provider().equals(provider());
    }

}
