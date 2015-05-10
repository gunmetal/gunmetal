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
import io.gunmetal.Singleton;
import io.gunmetal.Supplies;

@Module
public class FieldGunmetalBenchMarkModule {

    @Supplies(with = AA.class) public AA aa;
    @Supplies(with = BB.class) public BB bb;
    @Supplies(with = CC.class) public CC cc;
    @Supplies(with = DD.class) public DD dd;
    @Supplies(with = A.class) public A a;
    @Supplies(with = B.class) public B b;
    @Supplies(with = C.class) public C c;
    @Supplies(with = D.class) public D d;
    @Supplies(with = E.class) @Singleton public E e;
    @Supplies(with = F.class) public F f;
    @Supplies(with = G.class) public G g;
    @Supplies(with = H.class) public H h;
    @Supplies(with = I.class) public I i;
    @Supplies(with = J.class) public J j;
    @Supplies(with = K.class) public K k;
    @Supplies(with = L.class) public L l;
    @Supplies(with = M.class) public M m;
    @Supplies(with = N.class) public N n;
    @Supplies(with = O.class) public O o;
    @Supplies(with = P.class) public P p;
    @Supplies(with = Q.class) public Q q;
    @Supplies(with = R.class) public R r;
    @Supplies(with = S.class) public S s;
    @Supplies(with = T.class) public T t;
    @Supplies(with = U.class) public U u;
    @Supplies(with = V.class) public V v;
    @Supplies(with = W.class) public W w;
    @Supplies(with = X.class) public X x;
    @Supplies(with = Y.class) public Y y;
    @Supplies(with = Z.class) public Z z;

    @Module(dependsOn = FieldGunmetalBenchMarkModule.class)
    public interface Component {

        void inject(Object o);

        public interface Factory {
            Component create();
        }

    }

}
