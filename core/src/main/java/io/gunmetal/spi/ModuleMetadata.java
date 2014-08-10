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

import io.gunmetal.Module;

/**
 * @author rees.byars
 */
public final class ModuleMetadata {

    private final Class<?> moduleClass;
    private final Qualifier qualifier;
    private final Module module;

    public ModuleMetadata(Class<?> moduleClass, Qualifier qualifier, Module module) {
        this.moduleClass = moduleClass;
        this.qualifier = qualifier;
        this.module = module;
    }

    public Class<?> moduleClass() {
        return moduleClass;
    }

    public Qualifier qualifier() {
        return qualifier;
    }

    public Class<?>[] referencedModules() {
        return module.dependsOn();
    }

    public Module moduleAnnotation() {
        return module;
    }

    @Override public int hashCode() {
        return moduleClass().hashCode();
    }

    @Override public boolean equals(Object target) {
        return target instanceof ModuleMetadata && ((ModuleMetadata) target).moduleClass() == moduleClass();
    }

    @Override public String toString() {
        return "module[ " + qualifier() + ", class[ " + moduleClass.getName() + " ] ]";
    }

}
