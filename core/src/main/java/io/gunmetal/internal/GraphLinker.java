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
class GraphLinker implements Linkers {

    private final Queue<Linker> postWiringLinkers = new LinkedList<>();
    private final Queue<Linker> eagerLinkers = new LinkedList<>();

    @Override public synchronized void addWiringLinker(Linker linker) {
        postWiringLinkers.add(linker);
    }

    @Override public synchronized void addEagerLinker(Linker linker) {
        eagerLinkers.add(linker);
    }

    synchronized void linkGraph(InternalProvider internalProvider, ResolutionContext linkingContext) {
        while (!postWiringLinkers.isEmpty()) {
            postWiringLinkers.remove().link(internalProvider, linkingContext);
        }
    }

    synchronized void linkAll(InternalProvider internalProvider, ResolutionContext linkingContext) {
        while (!postWiringLinkers.isEmpty()) {
            postWiringLinkers.remove().link(internalProvider, linkingContext);
        }
        while (!eagerLinkers.isEmpty()) {
            eagerLinkers.remove().link(internalProvider, linkingContext);
        }
    }

}
