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

package io.gunmetal.testmocks;

import io.gunmetal.ApplicationModule;
import io.gunmetal.Component;
import io.gunmetal.Module;
import io.gunmetal.Prototype;

@ApplicationModule(modules = NewGunmetalBenchMarkModule2.class)
@Module(
        components = {
                @Component(type = AA.class, scope = Prototype.class),
                @Component(type = BB.class, scope = Prototype.class),
                @Component(type = CC.class, scope = Prototype.class),
                @Component(type = DD.class, scope = Prototype.class),
                @Component(type = A.class, scope = Prototype.class),
                @Component(type = B.class, scope = Prototype.class),
                @Component(type = C.class, scope = Prototype.class),
                @Component(type = D.class, scope = Prototype.class),
                @Component(type = E.class),
                @Component(type = F.class, scope = Prototype.class),
                @Component(type = G.class, scope = Prototype.class),
                @Component(type = H.class, scope = Prototype.class),
                @Component(type = I.class, scope = Prototype.class),
                @Component(type = J.class, scope = Prototype.class),
                @Component(type = K.class, scope = Prototype.class),
                @Component(type = L.class, scope = Prototype.class),
                @Component(type = M.class, scope = Prototype.class),
                @Component(type = N.class, scope = Prototype.class),
                @Component(type = O.class, scope = Prototype.class),
                @Component(type = P.class, scope = Prototype.class),
                @Component(type = Q.class, scope = Prototype.class),
                @Component(type = R.class, scope = Prototype.class),
                @Component(type = S.class, scope = Prototype.class),
                @Component(type = T.class, scope = Prototype.class),
                @Component(type = U.class, scope = Prototype.class),
                @Component(type = V.class, scope = Prototype.class),
                @Component(type = W.class, scope = Prototype.class),
                @Component(type = X.class, scope = Prototype.class),
                @Component(type = Y.class, scope = Prototype.class),
                @Component(type = Z.class, scope = Prototype.class)
        }
)
public class NewGunmetalBenchMarkModule2 { }
