package com.github.overengineer.container.benchmark;

import dagger.Module;
import dagger.Provides;

@Module(library = true, injects = A.class)
public class DaggerBenchMarkModule {

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

    @Provides E e(F f) {
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

    @Provides L l() {
        return new L();
    }

}
