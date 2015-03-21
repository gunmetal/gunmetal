package io.gunmetal.benchmarks;

import io.gunmetal.testmocks.*;
import com.google.inject.AbstractModule;

/**
 * @author rees.byars
 */
public class GuiceBenchMarkModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AA.class);
        bind(BB.class);
        bind(CC.class);
        bind(DD.class);
        bind(A.class);
        bind(B.class);
        bind(C.class);
        bind(D.class);
        bind(E.class);
        bind(F.class);
        bind(G.class);
        bind(H.class);
        bind(I.class);
        bind(J.class);
        bind(K.class);
        bind(L.class);
        bind(M.class);
        bind(N.class);
        bind(O.class);
        bind(P.class);
        bind(Q.class);
        bind(R.class);
        bind(S.class);
        bind(T.class);
        bind(U.class);
        bind(V.class);
        bind(W.class);
        bind(X.class);
        bind(Y.class);
    }
}
