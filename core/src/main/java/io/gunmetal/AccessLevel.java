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

import java.lang.reflect.Modifier;

/**
 * @author rees.byars
 */
public enum  AccessLevel {

    PRIVATE,
    PACKAGE_PRIVATE,
    PROTECTED,
    PUBLIC,
    UNDEFINED;

    public static AccessLevel get(int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return PUBLIC;
        } else if (Modifier.isPrivate(modifiers)) {
            return PRIVATE;
        } else if (Modifier.isProtected(modifiers)) {
            return PROTECTED;
        } else {
            return PACKAGE_PRIVATE;
        }
    }

    @Override public String toString() {
        return "accessLevel[ " + super.toString() + " ]";
    }

}
