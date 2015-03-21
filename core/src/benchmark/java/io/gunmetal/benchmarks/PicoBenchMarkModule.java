package io.gunmetal.benchmarks;

import io.gunmetal.testmocks.*;
import org.picocontainer.PicoContainer;
import org.picocontainer.containers.TransientPicoContainer;

/**
 * @author rees.byars
 */
public class PicoBenchMarkModule {

    public PicoContainer get() {
        return new TransientPicoContainer()
            .addComponent(AA.class)
            .addComponent(BB.class)
            .addComponent(CC.class)
            .addComponent(DD.class)
            .addComponent(A.class)
            .addComponent(B.class)
            .addComponent(C.class)
            .addComponent(D.class)
            .addComponent(E.class)
            .addComponent(F.class)
            .addComponent(G.class)
            .addComponent(H.class)
            .addComponent(I.class)
            .addComponent(J.class)
            .addComponent(K.class)
            .addComponent(L.class)
            .addComponent(M.class)
            .addComponent(N.class)
            .addComponent(O.class)
            .addComponent(P.class)
            .addComponent(Q.class)
            .addComponent(R.class)
            .addComponent(S.class)
            .addComponent(T.class)
            .addComponent(U.class)
            .addComponent(V.class)
            .addComponent(W.class)
            .addComponent(X.class)
            .addComponent(Y.class)
            .addComponent(Z.class);
    }
}
