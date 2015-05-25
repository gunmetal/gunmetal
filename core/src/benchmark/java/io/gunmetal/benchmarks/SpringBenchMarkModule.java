package io.gunmetal.benchmarks;

import io.gunmetal.sandbox.testmocks.A;
import io.gunmetal.sandbox.testmocks.AA;
import io.gunmetal.sandbox.testmocks.B;
import io.gunmetal.sandbox.testmocks.BB;
import io.gunmetal.sandbox.testmocks.C;
import io.gunmetal.sandbox.testmocks.CC;
import io.gunmetal.sandbox.testmocks.D;
import io.gunmetal.sandbox.testmocks.DD;
import io.gunmetal.sandbox.testmocks.E;
import io.gunmetal.sandbox.testmocks.F;
import io.gunmetal.sandbox.testmocks.G;
import io.gunmetal.sandbox.testmocks.H;
import io.gunmetal.sandbox.testmocks.I;
import io.gunmetal.sandbox.testmocks.J;
import io.gunmetal.sandbox.testmocks.K;
import io.gunmetal.sandbox.testmocks.L;
import io.gunmetal.sandbox.testmocks.M;
import io.gunmetal.sandbox.testmocks.N;
import io.gunmetal.sandbox.testmocks.O;
import io.gunmetal.sandbox.testmocks.P;
import io.gunmetal.sandbox.testmocks.Q;
import io.gunmetal.sandbox.testmocks.R;
import io.gunmetal.sandbox.testmocks.S;
import io.gunmetal.sandbox.testmocks.T;
import io.gunmetal.sandbox.testmocks.U;
import io.gunmetal.sandbox.testmocks.V;
import io.gunmetal.sandbox.testmocks.W;
import io.gunmetal.sandbox.testmocks.X;
import io.gunmetal.sandbox.testmocks.Y;
import io.gunmetal.sandbox.testmocks.Z;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * @author rees.byars
 */
@Configuration
public class SpringBenchMarkModule {

    @Scope("prototype") @Bean public AA aa(A a, BB bb, R r, E e, E ee, S s) {
        return new AA(a, bb, r, e, ee, s);
    }

    @Scope("prototype") @Bean public BB bb(B b, CC cc, R r, E e, E ee, S s) {
        return new BB(b, cc, r, e, ee, s);
    }

    @Scope("prototype") @Bean public CC cc(C c, DD dd, R r, E e, E ee, S s) {
        return new CC(c, dd, r, e, ee, s);
    }

    @Scope("prototype") @Bean public DD dd(D d, R r, E e, E ee, S s) {
        return new DD(d, r, e, ee, s);
    }

    @Scope("prototype") @Bean public A a(B b) {
        return new A(b);
    }

    @Scope("prototype") @Bean public B b(C c) {
        return new B(c);
    }

    @Scope("prototype") @Bean public C c(D d) {
        return new C(d);
    }

    @Scope("prototype") @Bean public D d(E e) {
        return new D(e);
    }

    @Scope("singleton") @Bean public E e(F f) {
        return new E(f);
    }

    @Scope("prototype") @Bean public F f(G g) {
        return new F(g);
    }

    @Scope("prototype") @Bean public G g(H h) {
        return new G(h);
    }

    @Scope("prototype") @Bean public H h(I i) {
        return new H(i);
    }

    @Scope("prototype") @Bean public I i(J j) {
        return new I(j);
    }

    @Scope("prototype") @Bean public J j(K k) {
        return new J(k);
    }

    @Scope("prototype") @Bean public K k(L l) {
        return new K(l);
    }

    @Scope("prototype") @Bean public L l(M m) {
        return new L(m);
    }

    @Scope("prototype") @Bean public M m(N n) {
        return new M(n);
    }

    @Scope("prototype") @Bean public N n(O o) {
        return new N(o);
    }

    @Scope("prototype") @Bean public O o(P p) {
        return new O(p);
    }

    @Scope("prototype") @Bean public P p(Q q) {
        return new P(q);
    }

    @Scope("prototype") @Bean public Q q(R r) {
        return new Q(r);
    }

    @Scope("prototype") @Bean public R r(S s) {
        return new R(s);
    }

    @Scope("prototype") @Bean public S s(T t) {
        return new S(t);
    }

    @Scope("prototype") @Bean public T t(U u) {
        return new T(u);
    }

    @Scope("prototype") @Bean public U u(V v) {
        return new U(v);
    }

    @Scope("prototype") @Bean public V v(W w) {
        return new V(w);
    }

    @Scope("prototype") @Bean public W w(X x) {
        return new W(x);
    }

    @Scope("prototype") @Bean public X x(Y y) {
        return new X(y);
    }

    @Scope("prototype") @Bean public Y y(Z z) {
        return new Y(z);
    }

    @Scope("prototype") @Bean public Z z() {
        return new Z();
    }
}
