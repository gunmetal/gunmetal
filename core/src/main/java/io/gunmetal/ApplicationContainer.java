package io.gunmetal;

import com.github.overengineer.gunmetal.key.Dependency;

import java.lang.annotation.Annotation;

/**
 * @author rees.byars
 */
public interface ApplicationContainer {
    ApplicationContainer inject(Object injectionTarget);
    <T> T get(Class<T> clazz);
    <T> T get(Class<T> clazz, Annotation ... qualifier);
    <T> T get(Dependency<T> dependency);
}
