package com.github.overengineer.container;

import com.github.overengineer.container.key.Dependency;

import java.io.Serializable;
import java.util.List;

/**
 * @author rees.byars
 */
public interface Provider extends Serializable {
    <T> T get(Class<T> clazz, SelectionAdvisor ... advisors);
    <T> T get(Class<T> clazz, Object qualifier, SelectionAdvisor ... advisors);
    <T> T get(Dependency<T> dependency, SelectionAdvisor ... advisors);
    <T> List<T> getAll(Class<T> clazz, SelectionAdvisor ... advisors);
    <T> List<T> getAll(Class<T> clazz, Object qualifier, SelectionAdvisor ... advisors);
    <T> List<T> getAll(Dependency<T> dependency, SelectionAdvisor ... advisors);
}
