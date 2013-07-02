package com.github.overengineer.gunmetal;

/**
 * @author rees.byars
 */
public interface SelectionAdvisor {
    boolean validSelection(ComponentStrategy<?> candidateStrategy);
    SelectionAdvisor[] NONE = new SelectionAdvisor[]{};
}
