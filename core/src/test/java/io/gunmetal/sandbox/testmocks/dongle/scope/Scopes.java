package io.gunmetal.sandbox.testmocks.dongle.scope;

import io.gunmetal.spi.Scope;

/**
 * @author rees.byars
 */
public enum Scopes implements Scope {

    THREAD(0);

    int order;

    Scopes(int order) {
        this.order = order;
    }

    @Override public boolean canInject(Scope scope) {
        return scope instanceof Scopes && ((Scopes) scope).order >= order;
    }

}
