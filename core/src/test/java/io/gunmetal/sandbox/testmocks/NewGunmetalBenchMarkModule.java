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
import io.gunmetal.Provides;
import io.gunmetal.Singleton;

@Module
public class NewGunmetalBenchMarkModule {

    @Provides public static AA aa(A a, BB bb, R r, E e, E ee, S s) {
        return new AA(a, bb, r, e, ee, s);
    }

    @Provides public static BB bb(B b, CC cc, R r, E e, E ee, S s) {
        return new BB(b, cc, r, e, ee, s);
    }

    @Provides public static CC cc(C c, DD dd, R r, E e, E ee, S s) {
        return new CC(c, dd, r, e, ee, s);
    }

    @Provides public static DD dd(D d, R r, E e, E ee, S s) {
        return new DD(d, r, e, ee, s);
    }

    @Provides public static A a(B b) {
        return new A(b);
    }

    @Provides public static B b(C c) {
        return new B(c);
    }

    @Provides public static C c(D d) {
        return new C(d);
    }

    @Provides public static D d(E e) {
        return new D(e);
    }

    @Singleton @Provides public static E e(F f) {
        return new E(f);
    }

    @Provides public static F f(G g) {
        return new F(g);
    }

    @Provides public static G g(H h) {
        return new G(h);
    }

    @Provides public static H h(I i) {
        return new H(i);
    }

    @Provides public static I i(J j) {
        return new I(j);
    }

    @Provides public static J j(K k) {
        return new J(k);
    }

    @Provides public static K k(L l) {
        return new K(l);
    }

    @Provides public static L l(M m) {
        return new L(m);
    }

    @Provides public static M m(N n) {
        return new M(n);
    }

    @Provides public static N n(O o) {
        return new N(o);
    }

    @Provides public static O o(P p) {
        return new O(p);
    }

    @Provides public static P p(Q q) {
        return new P(q);
    }

    @Provides public static Q q(R r) {
        return new Q(r);
    }

    @Provides public static R r(S s) {
        return new R(s);
    }

    @Provides public static S s(T t) {
        return new S(t);
    }

    @Provides public static T t(U u) {
        return new T(u);
    }

    @Provides public static U u(V v) {
        return new U(v);
    }

    @Provides public static V v(W w) {
        return new V(w);
    }

    @Provides public static W w(X x) {
        return new W(x);
    }

    @Provides public static X x(Y y) {
        return new X(y);
    }

    @Provides public static Y y(Z z) {
        return new Y(z);
    }

    @Provides public static Z z() {
        return new Z();
    }

}
