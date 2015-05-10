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

package io.gunmetal.sandbox.testmocks;

import io.gunmetal.Module;
import io.gunmetal.Supplies;

import java.util.function.Supplier;

@Module
public class SlimGunmetalBenchMarkModule {

    @Supplies public static AA aa(A a, BB bb, R r, E e, E ee, S s) {
        return new AA(a, bb, r, e, ee, s);
    }

    @Module(dependsOn = SlimGunmetalBenchMarkModule.class)
    public interface SlimComponent {

        void inject(Object o);

        Supplier<N> supplier();

        public interface Factory {
            SlimComponent create();
        }

    }


}
