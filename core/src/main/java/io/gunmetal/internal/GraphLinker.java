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

    final Queue<Linker> postWiringLinkers = new LinkedList<>();
    final Queue<Linker> statefulSourceLinkers = new LinkedList<>();
    final Queue<Linker> eagerLinkers = new LinkedList<>();

    @Override public void addWiringLinker(Linker linker) {
        postWiringLinkers.add(linker);
    }

    @Override public void addStatefulSourceLinker(Linker linker) {
        statefulSourceLinkers.add(linker);
    }

    @Override public void addEagerLinker(Linker linker) {
        eagerLinkers.add(linker);
    }

    void linkGraph(InternalProvider internalProvider, ResolutionContext linkingContext) {
        while (!postWiringLinkers.isEmpty()) {
            postWiringLinkers.remove().link(internalProvider, linkingContext);
        }
    }

    void linkAll(InternalProvider internalProvider, ResolutionContext linkingContext) {
        while (!postWiringLinkers.isEmpty()) {
            postWiringLinkers.remove().link(internalProvider, linkingContext);
        }
        while (!statefulSourceLinkers.isEmpty()) {
            statefulSourceLinkers.remove().link(internalProvider, linkingContext);
        }
        while (!eagerLinkers.isEmpty()) {
            eagerLinkers.remove().link(internalProvider, linkingContext);
        }
    }

}
