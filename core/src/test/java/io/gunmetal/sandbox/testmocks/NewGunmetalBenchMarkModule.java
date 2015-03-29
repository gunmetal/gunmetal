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
import io.gunmetal.Singleton;

@Module
public class NewGunmetalBenchMarkModule {

    @Supplies public static AA aa(A a, BB bb, R r, E e, E ee, S s) {
        return new AA(a, bb, r, e, ee, s);
    }

    @Supplies public static BB bb(B b, CC cc, R r, E e, E ee, S s) {
        return new BB(b, cc, r, e, ee, s);
    }

    @Supplies public static CC cc(C c, DD dd, R r, E e, E ee, S s) {
        return new CC(c, dd, r, e, ee, s);
    }

    @Supplies public static DD dd(D d, R r, E e, E ee, S s) {
        return new DD(d, r, e, ee, s);
    }

    @Supplies public static A a(B b) {
        return new A(b);
    }

    @Supplies public static B b(C c) {
        return new B(c);
    }

    @Supplies public static C c(D d) {
        return new C(d);
    }

    @Supplies public static D d(E e) {
        return new D(e);
    }

    @Singleton @Supplies public static E e(F f) {
        return new E(f);
    }

    @Supplies public static F f(G g) {
        return new F(g);
    }

    @Supplies public static G g(H h) {
        return new G(h);
    }

    @Supplies public static H h(I i) {
        return new H(i);
    }

    @Supplies public static I i(J j) {
        return new I(j);
    }

    @Supplies public static J j(K k) {
        return new J(k);
    }

    @Supplies public static K k(L l) {
        return new K(l);
    }

    @Supplies public static L l(M m) {
        return new L(m);
    }

    @Supplies public static M m(N n) {
        return new M(n);
    }

    @Supplies public static N n(O o) {
        return new N(o);
    }

    @Supplies public static O o(P p) {
        return new O(p);
    }

    @Supplies public static P p(Q q) {
        return new P(q);
    }

    @Supplies public static Q q(R r) {
        return new Q(r);
    }

    @Supplies public static R r(S s) {
        return new R(s);
    }

    @Supplies public static S s(T t) {
        return new S(t);
    }

    @Supplies public static T t(U u) {
        return new T(u);
    }

    @Supplies public static U u(V v) {
        return new U(v);
    }

    @Supplies public static V v(W w) {
        return new V(w);
    }

    @Supplies public static W w(X x) {
        return new W(x);
    }

    @Supplies public static X x(Y y) {
        return new X(y);
    }

    @Supplies public static Y y(Z z) {
        return new Y(z);
    }

    @Supplies public static Z z() {
        return new Z();
    }

}
