package com.github.overengineer.gunmetal;

import java.io.Serializable;

/**
 * @author rees.byars
 */
public interface ComponentPostProcessor extends Serializable {

    <T> T postProcess(T component);

}
