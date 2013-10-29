package io.gunmetal.builder;

import com.github.overengineer.gunmetal.InternalProvider;

/**
 * @author rees.byars
 */
public interface PhaseOneCallback {
    void afterPhaseOne(InternalProvider provider);
}
