package com.github.overengineer.container.benchmark;

import javax.inject.Inject;

/**
 * @author rees.byars
 */
public class CC {
    @Inject
    public CC(C c, @BenchMarkModule.Member DD dd, R r, E e, E ee, S s) { }
}
