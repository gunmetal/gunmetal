package com.github.overengineer.gunmetal;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ComponentInitializationListener extends Serializable {

    <T> T onInitialization(T component);

}
