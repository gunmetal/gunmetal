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

package io.gunmetal;

/**
 * @author rees.byars
 */
public enum Option {
    REQUIRE_QUALIFIERS,
    DISALLOW_REFLECTIVE_INSTANTIATION,
    REQUIRE_INTERFACES,
    ENABLE_AOP,
    REQUIRE_ACYCLIC_GRAPH,
    JSR_330_STRICT,
    JSR_330_WITH_MULTIPLE_QUALIFIERS,
    RESTRICT_FIELD_INJECTION,
    RESTRICT_SETTER_INJECTION
}
