package com.github.overengineer.gunmetal.benchmark;

import com.github.overengineer.gunmetal.testmocks.*;
import com.github.overengineer.gunmetal.module.BaseModule;
import com.github.overengineer.gunmetal.scope.Scopes;


/**
 * @author rees.byars
 */
public class GunmetalBenchMarkModule extends BaseModule {

    @Override
    public void configure() {
        
        defaultScope(Scopes.PROTOTYPE);

        use(AA.class);
        use(BB.class);
        use(CC.class);
        use(DD.class);
        use(A.class);
        use(B.class);
        use(C.class);
        use(D.class);
        use(E.class).withScope(Scopes.SINGLETON);
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
