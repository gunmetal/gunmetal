package com.github.overengineer.gunmetal.module;

import com.github.overengineer.gunmetal.key.Dependency;
import com.github.overengineer.gunmetal.key.Generic;

import java.util.LinkedList;
import java.util.List;

/**
 * @author rees.byars
 */
public class TargetedComponent<T> {

    private final T component;
    private final List<Class<? super T>> targetClasses = new LinkedList<Class<? super T>>();
    private final List<Dependency<? super T>> targetDependencies = new LinkedList<Dependency<? super T>>();

    private TargetedComponent(T component) {
        this.component = component;
    }

    public T getComponent() {
        return component;
    }

    protected TargetedComponent<T> forType(Class<? super T> targetClass) {
        targetClasses.add(targetClass);
        return this;
    }

    protected TargetedComponent<T> forType(Generic<? super T> targetDependency) {
        targetDependencies.add(targetDependency);
        return this;
    }

    public static <T> TargetedComponent<T> use(T component) {
        return new TargetedComponent<T>(component);
    }

    public List<Class<? super T>> getTargetClasses() {
        return targetClasses;
    }

    public List<Dependency<? super T>> getTargetDependencies() {
        return targetDependencies;
    }

}
