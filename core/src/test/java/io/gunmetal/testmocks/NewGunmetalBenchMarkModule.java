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
import io.gunmetal.Module;
import io.gunmetal.Prototype;

@ApplicationModule(modules = NewGunmetalBenchMarkModule.class)
@Module
public class NewGunmetalBenchMarkModule {

    @Prototype static AA aa(A a, BB bb, R r, E e, E ee, S s) {
        return new AA(a, bb, r, e, ee, s);
    }

    @Prototype static BB bb(B b, CC cc, R r, E e, E ee, S s) {
        return new BB(b, cc, r, e, ee, s);
    }

    @Prototype static CC cc(C c, DD dd, R r, E e, E ee, S s) {
        return new CC(c, dd, r, e, ee, s);
    }

    @Prototype static DD dd(D d, R r, E e, E ee, S s) {
        return new DD(d, r, e, ee, s);
    }

    @Prototype static A a(B b) {
        return new A(b);
    }

    @Prototype static B b(C c) {
        return new B(c);
    }

    @Prototype static C c(D d) {
        return new C(d);
    }

    @Prototype static D d(E e) {
        return new D(e);
    }

    static E e(F f) {
        return new E(f);
    }

    @Prototype static F f(G g) {
        return new F(g);
    }

    @Prototype static G g(H h) {
        return new G(h);
    }

    @Prototype static H h(I i) {
        return new H(i);
    }

    @Prototype static I i(J j) {
        return new I(j);
    }

    @Prototype static J j(K k) {
        return new J(k);
    }

    @Prototype static K k(L l) {
        return new K(l);
    }

    @Prototype static L l(M m) {
        return new L(m);
    }

    @Prototype static M m(N n) {
        return new M(n);
    }

    @Prototype static N n(O o) {
        return new N(o);
    }

    @Prototype static O o(P p) {
        return new O(p);
    }

    @Prototype static P p(Q q) {
        return new P(q);
    }

    @Prototype static Q q(R r) {
        return new Q(r);
    }

    @Prototype static R r(S s) {
        return new R(s);
    }

    @Prototype static S s(T t) {
        return new S(t);
    }

    @Prototype static T t(U u) {
        return new T(u);
    }

    @Prototype static U u(V v) {
        return new U(v);
    }

    @Prototype static V v(W w) {
        return new V(w);
    }

    @Prototype static W w(X x) {
        return new W(x);
    }

    @Prototype static X x(Y y) {
        return new X(y);
    }

    @Prototype static Y y(Z z) {
        return new Y(z);
    }

    @Prototype static Z z() {
        return new Z();
    }

}
