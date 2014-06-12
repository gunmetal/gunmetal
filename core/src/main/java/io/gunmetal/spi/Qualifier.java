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
public interface Qualifier {

    // TODO revisit the intersects impl here
    Qualifier NONE = new Qualifier() {

        Object[] qualifiers = {};

        @Override public Object[] qualifiers() {
            return qualifiers;
        }

        @Override public Qualifier merge(Qualifier other) {
            return other;
        }

        @Override public boolean intersects(Object[] qualifiers) {
            return true;
        }

        @Override public boolean intersects(Qualifier qualifier) {
            return true;
        }

        @Override public String toString() {
            return "qualifier[ NONE ]";
        }

    };

    Object[] qualifiers();

    Qualifier merge(Qualifier other);

    boolean intersects(Object[] qualifiers);

    boolean intersects(Qualifier qualifier);

}
