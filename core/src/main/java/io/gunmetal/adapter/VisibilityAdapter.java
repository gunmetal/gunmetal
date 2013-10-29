package io.gunmetal.adapter;

/**
 * @author rees.byars
 */
public interface VisibilityAdapter<T> {
    boolean isVisibleTo(T target);
}
