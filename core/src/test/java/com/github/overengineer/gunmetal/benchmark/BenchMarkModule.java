package com.github.overengineer.gunmetal.benchmark;

import com.github.overengineer.gunmetal.metadata.Qualifier;
import com.github.overengineer.gunmetal.module.BaseModule;
import com.github.overengineer.gunmetal.scope.Scopes;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * @author rees.byars
 */
public class BenchMarkModule extends BaseModule {

    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Qualifier
    public @interface Member { }

    @Override
    public void configure() {
        
        defaultScope(Scopes.PROTOTYPE);

        defaultQualifier(Member.class);

        use(AA.class);
        use(BB.class);
        use(CC.class);
        use(DD.class);
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
