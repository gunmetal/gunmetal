package com.github.overengineer.container.benchmark;

import com.github.overengineer.container.module.BaseModule;
import com.github.overengineer.container.scope.Scopes;

/**
 * @author rees.byars
 */
public class BenchMarkModule extends BaseModule {
    @Override
    protected void configure() {
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
    }
}
