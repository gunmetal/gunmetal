package com.github.overengineer.container.benchmark;

import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.util.Scoped;

/**
 * @author rees.byars
 */
public class SilkBenchMarkModule extends BinderModule {
    @Override
    protected void declare() {
        per(Scoped.INJECTION).bind(AA.class).toConstructor();
        per(Scoped.INJECTION).bind(BB.class).toConstructor();
        per(Scoped.INJECTION).bind(CC.class).toConstructor();
        per(Scoped.INJECTION).bind(A.class).toConstructor();
        per(Scoped.INJECTION).bind(B.class).toConstructor();
        per(Scoped.INJECTION).bind(C.class).toConstructor();
        per(Scoped.INJECTION).bind(D.class).toConstructor();
        per(Scoped.INJECTION).bind(E.class).toConstructor();
        per(Scoped.INJECTION).bind(F.class).toConstructor();
        per(Scoped.INJECTION).bind(G.class).toConstructor();
        per(Scoped.INJECTION).bind(H.class).toConstructor();
        per(Scoped.INJECTION).bind(I.class).toConstructor();
        per(Scoped.INJECTION).bind(J.class).toConstructor();
        per(Scoped.INJECTION).bind(K.class).toConstructor();
        per(Scoped.INJECTION).bind(L.class).toConstructor();
        per(Scoped.INJECTION).bind(M.class).toConstructor();
        per(Scoped.INJECTION).bind(N.class).toConstructor();
        per(Scoped.INJECTION).bind(O.class).toConstructor();
        per(Scoped.INJECTION).bind(P.class).toConstructor();
        per(Scoped.INJECTION).bind(Q.class).toConstructor();
        per(Scoped.INJECTION).bind(R.class).toConstructor();
        per(Scoped.INJECTION).bind(S.class).toConstructor();
        per(Scoped.INJECTION).bind(T.class).toConstructor();
        per(Scoped.INJECTION).bind(U.class).toConstructor();
        per(Scoped.INJECTION).bind(V.class).toConstructor();
        per(Scoped.INJECTION).bind(W.class).toConstructor();
        per(Scoped.INJECTION).bind(X.class).toConstructor();
        per(Scoped.INJECTION).bind(Y.class).toConstructor();
        per(Scoped.INJECTION).bind(Z.class).toConstructor();
    }
}
