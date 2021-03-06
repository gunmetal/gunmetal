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

/**
 * @author rees.byars
 */
public interface ResolutionContext {

    ProvisionContext provisionContext(ResourceMetadata<?> resourceMetadata);

    void setParam(Dependency dependency, Object value);

    Object getParam(Dependency dependency);

    boolean hasParam(Dependency dependency);

    interface States {
        byte NEW = 0;
        byte PRE_INSTANTIATION = 1;
        byte PRE_INJECTION = 2;
    }

    final class ProvisionContext {
        public byte state = States.NEW;
        public Object provision;
        public boolean attemptedCircularResolution = false;
    }

}
