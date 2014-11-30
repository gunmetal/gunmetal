package io.gunmetal.compiler;

/**
 * @author rees.byars
 */
public interface Scope {

    enum Defaults implements Scope {

        PROTOTYPE,

        SINGLETON

    }

}
