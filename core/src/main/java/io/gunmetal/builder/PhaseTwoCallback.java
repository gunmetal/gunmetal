package io.gunmetal.builder;

import com.github.overengineer.gunmetal.InternalProvider;

/**
 * @author rees.byars
 */
public interface PhaseTwoCallback {
    void afterPhaseTwo(InternalProvider provider);
}
