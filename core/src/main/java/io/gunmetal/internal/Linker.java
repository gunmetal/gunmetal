package io.gunmetal.internal;

/**
 * @author rees.byars
 */
interface Linker {
    void link(InternalProvider internalProvider, ResolutionContext linkingContext);
}
