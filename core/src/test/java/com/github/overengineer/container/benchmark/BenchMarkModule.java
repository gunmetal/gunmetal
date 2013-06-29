package com.github.overengineer.container.benchmark;

import com.github.overengineer.container.module.BaseModule;
import com.github.overengineer.container.scope.Scopes;

/**
 * @author rees.byars
 */
public class BenchMarkModule extends BaseModule {
    @Override
    protected void configure() {
        
        defaultScope(Scopes.PROTOTYPE);
        
        use(AA.class);
        use(BB.class);
        use(CC.class);
        use(A.class);
        use(B.class);
        use(C.class);
        use(D.class);
        use(E.class);
        use(F.class);
        use(G.class);
        use(H.class);
        use(I.class);
        use(J.class);
        use(K.class);
        use(L.class);
        use(M.class);
        use(N.class);
        use(O.class);
        use(P.class);
        use(Q.class);
        use(R.class);
        use(S.class);
        use(T.class);
        use(U.class);
        use(V.class);
        use(W.class);
        use(X.class);
        use(Y.class);
        use(Z.class);
    }
}
