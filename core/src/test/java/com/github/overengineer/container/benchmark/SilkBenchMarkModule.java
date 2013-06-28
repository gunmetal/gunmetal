package com.github.overengineer.container.benchmark;

import se.jbee.inject.bind.BinderModule;

/**
 * @author rees.byars
 */
public class SilkBenchMarkModule extends BinderModule {
    @Override
    protected void declare() {
        bind(A.class).toConstructor();
        bind(B.class).toConstructor();
        bind(C.class).toConstructor();
        bind(D.class).toConstructor();
        bind(E.class).toConstructor();
        bind(F.class).toConstructor();
        bind(G.class).toConstructor();
        bind(H.class).toConstructor();
        bind(I.class).toConstructor();
        bind(J.class).toConstructor();
        bind(K.class).toConstructor();
        bind(L.class).toConstructor();
        bind(M.class).toConstructor();
        bind(N.class).toConstructor();
        bind(O.class).toConstructor();
        bind(P.class).toConstructor();
        bind(Q.class).toConstructor();
        bind(R.class).toConstructor();
        bind(S.class).toConstructor();
        bind(T.class).toConstructor();
        bind(U.class).toConstructor();
        bind(V.class).toConstructor();
        bind(W.class).toConstructor();
        bind(X.class).toConstructor();
        bind(Y.class).toConstructor();
        bind(Z.class).toConstructor();
    }
}
