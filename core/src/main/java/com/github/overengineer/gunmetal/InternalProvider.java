package com.github.overengineer.gunmetal;

import com.github.overengineer.gunmetal.key.Dependency;

import java.util.List;

/**
 * @author rees.byars
 */
public interface InternalProvider {
    <T> T get(Dependency<T> dependency, ResolutionContext resolutionContext, SelectionAdvisor ... advisors);
    <T> ComponentStrategy<T> getStrategy(Dependency<T> key, SelectionAdvisor ... advisors);
    <T> List<ComponentStrategy<T>> getAllStrategies(Dependency<T> dependency, SelectionAdvisor... advisors);
}
