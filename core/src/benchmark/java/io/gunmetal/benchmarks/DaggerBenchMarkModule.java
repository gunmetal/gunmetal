package io.gunmetal.benchmarks;

import io.gunmetal.testmocks.*;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module(library = true, injects = { AA.class, E.class, F.class, N.class, R.class, CaliperBenchmarks.class } )
public class DaggerBenchMarkModule {

    @Provides AA aa(A a, BB bb, R r, E e, E ee, S s) {
        return new AA(a, bb, r, e, ee, s);
    }

    @Provides BB bb(B b, CC cc, R r, E e, E ee, S s) {
        return new BB(b, cc, r, e, ee, s);
    }

    @Provides CC cc(C c, DD dd, R r, E e, E ee, S s) {
        return new CC(c, dd, r, e, ee, s);
    }

    @Provides DD dd(D d, R r, E e, E ee, S s) {
        return new DD(d, r, e, ee, s);
    }

    @Provides A a(B b) {
        return new A(b);
    }

    @Provides B b(C c) {
        return new B(c);
    }

    @Provides C c(D d) {
        return new C(d);
    }

    @Provides D d(E e) {
        return new D(e);
    }

    @Singleton @Provides E e(F f) {
        return new E(f);
    }

    @Provides F f(G g) {
        return new F(g);
    }

    @Provides G g(H h) {
        return new G(h);
    }

    @Provides H h(I i) {
        return new H(i);
    }

    @Provides I i(J j) {
        return new I(j);
    }

    @Provides J j(K k) {
        return new J(k);
    }

    @Provides K k(L l) {
        return new K(l);
    }

    @Provides L l(M m) {
        return new L(m);
    }

    @Provides M m(N n) {
        return new M(n);
    }

    @Provides N n(O o) {
        return new N(o);
    }

    @Provides O o(P p) {
        return new O(p);
    }

    @Provides P p(Q q) {
        return new P(q);
    }

    @Provides Q q(R r) {
        return new Q(r);
    }

    @Provides R r(S s) {
        return new R(s);
    }

    @Provides S s(T t) {
        return new S(t);
    }

    @Provides T t(U u) {
        return new T(u);
    }

    @Provides U u(V v) {
        return new U(v);
    }

    @Provides V v(W w) {
        return new V(w);
    }

    @Provides W w(X x) {
        return new W(x);
    }

    @Provides X x(Y y) {
        return new X(y);
    }

    @Provides Y y(Z z) {
        return new Y(z);
    }

    @Provides Z z() {
        return new Z();
    }

}
