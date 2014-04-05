package io.gunmetal.benchmarks;

import io.gunmetal.testmocks.*;

import se.jbee.inject.bind.BinderModule;
import se.jbee.inject.util.Scoped;

/**
 * @author rees.byars
 */
public class SilkBenchMarkModule extends BinderModule {

    public SilkBenchMarkModule() {
        super(Scoped.INJECTION);
    }
    
    @Override
    protected void declare() {
        bind(AA.class).toConstructor();
        bind(BB.class).toConstructor();
        bind(CC.class).toConstructor();
        bind(DD.class).toConstructor();
        bind(A.class).toConstructor();
        bind(B.class).toConstructor();
        bind(C.class).toConstructor();
        bind(D.class).toConstructor();
        per(Scoped.INJECTION).bind(E.class).toConstructor();
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
