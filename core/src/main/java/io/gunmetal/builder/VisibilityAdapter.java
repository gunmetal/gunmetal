package io.gunmetal.builder;

/**
 * @author rees.byars
 */
interface VisibilityAdapter<T> {
    boolean isVisibleTo(T target);
}
