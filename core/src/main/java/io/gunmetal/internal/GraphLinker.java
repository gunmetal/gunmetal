package io.gunmetal.internal;

import io.gunmetal.spi.InternalProvider;
import io.gunmetal.spi.Linker;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ResolutionContext;

import java.util.LinkedList;
import java.util.Queue;

/**
* @author rees.byars
*/
class GraphLinker implements Linkers, Linker {

    final Queue<WiringLinker> postWiringLinkers = new LinkedList<>();
    final Queue<EagerLinker> eagerLinkers = new LinkedList<>();

    @Override public void addWiringLinker(WiringLinker linker) {
        postWiringLinkers.add(linker);
    }

    @Override public void addEagerLinker(EagerLinker linker) {
        eagerLinkers.add(linker);
    }

    @Override public void link(InternalProvider internalProvider, ResolutionContext linkingContext) {
        while (!postWiringLinkers.isEmpty()) {
            postWiringLinkers.remove().link(internalProvider, linkingContext);
        }
        while (!eagerLinkers.isEmpty()) {
            eagerLinkers.remove().link(internalProvider, linkingContext);
        }
    }

}
