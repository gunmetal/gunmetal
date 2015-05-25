package io.gunmetal.internal;

import io.gunmetal.spi.DependencySupplier;
import io.gunmetal.spi.Linker;
import io.gunmetal.spi.Linkers;
import io.gunmetal.spi.ResolutionContext;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author rees.byars
 */
class ComponentLinker implements Linkers {

    private final Queue<Linker> postWiringLinkers = new LinkedList<>();
    private final Queue<Linker> eagerLinkers = new LinkedList<>();

    ComponentLinker() {
    }

    @Override public synchronized void addWiringLinker(Linker linker) {
        postWiringLinkers.add(linker);
    }

    @Override public synchronized void addEagerLinker(Linker linker) {
        eagerLinkers.add(linker);
    }

    synchronized void linkGraph(DependencySupplier dependencySupplier, ResolutionContext linkingContext) {
        while (!postWiringLinkers.isEmpty()) {
            postWiringLinkers.remove().link(dependencySupplier, linkingContext);
        }
    }

    synchronized void linkAll(DependencySupplier dependencySupplier, ResolutionContext linkingContext) {
        while (!postWiringLinkers.isEmpty()) {
            postWiringLinkers.remove().link(dependencySupplier, linkingContext);
        }
        while (!eagerLinkers.isEmpty()) {
            eagerLinkers.remove().link(dependencySupplier, linkingContext);
        }
    }

}
