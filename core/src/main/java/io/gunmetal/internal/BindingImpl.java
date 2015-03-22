package io.gunmetal.internal;

import io.gunmetal.spi.Dependency;

import java.util.List;

/**
 * @author rees.byars
 */
class BindingImpl implements Binding {

    private final Resource resource;
    private final List<Dependency> targets;

    BindingImpl(Resource resource,
                List<Dependency> targets) {
        this.resource = resource;
        this.targets = targets;
    }

    @Override public List<Dependency> targets() {
        return targets;
    }

    @Override public Resource resource() {
        return resource;
    }

    @Override public Binding replicateWith(GraphContext context) {
        return new BindingImpl(resource.replicateWith(context), targets);
    }

}
