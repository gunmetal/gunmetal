package com.github.overengineer.container.benchmark;

import com.github.overengineer.container.module.BaseModule;
import com.github.overengineer.container.scope.Scopes;

/**
 * @author rees.byars
 */
public class BenchMarkModule extends BaseModule {
    @Override
    protected void configure() {
        use(AA.class).withScope(Scopes.PROTOTYPE).forType(AA.class);
        use(BB.class).withScope(Scopes.PROTOTYPE).forType(BB.class);
        use(A.class).withScope(Scopes.PROTOTYPE).forType(A.class);
        use(B.class).withScope(Scopes.PROTOTYPE).forType(B.class);
        use(C.class).withScope(Scopes.PROTOTYPE).forType(C.class);
        use(D.class).withScope(Scopes.PROTOTYPE).forType(D.class);
        use(E.class).withScope(Scopes.PROTOTYPE).forType(E.class);
        use(F.class).withScope(Scopes.PROTOTYPE).forType(F.class);
        use(G.class).withScope(Scopes.PROTOTYPE).forType(G.class);
        use(H.class).withScope(Scopes.PROTOTYPE).forType(H.class);
        use(I.class).withScope(Scopes.PROTOTYPE).forType(I.class);
        use(J.class).withScope(Scopes.PROTOTYPE).forType(J.class);
        use(K.class).withScope(Scopes.PROTOTYPE).forType(K.class);
        use(L.class).withScope(Scopes.PROTOTYPE).forType(L.class);
        use(M.class).withScope(Scopes.PROTOTYPE).forType(M.class);
        use(N.class).withScope(Scopes.PROTOTYPE).forType(N.class);
        use(O.class).withScope(Scopes.PROTOTYPE).forType(O.class);
        use(P.class).withScope(Scopes.PROTOTYPE).forType(P.class);
        use(Q.class).withScope(Scopes.PROTOTYPE).forType(Q.class);
        use(R.class).withScope(Scopes.PROTOTYPE).forType(R.class);
        use(S.class).withScope(Scopes.PROTOTYPE).forType(S.class);
        use(T.class).withScope(Scopes.PROTOTYPE).forType(T.class);
        use(U.class).withScope(Scopes.PROTOTYPE).forType(U.class);
        use(V.class).withScope(Scopes.PROTOTYPE).forType(V.class);
        use(W.class).withScope(Scopes.PROTOTYPE).forType(W.class);
        use(X.class).withScope(Scopes.PROTOTYPE).forType(X.class);
        use(Y.class).withScope(Scopes.PROTOTYPE).forType(Y.class);
        use(Z.class).withScope(Scopes.PROTOTYPE).forType(Z.class);
    }
}
